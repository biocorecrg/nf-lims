#!/usr/bin/env python3
import http.server
import socketserver
import json
import sys

PORT = 28081

class MockLIMSHandler(http.server.BaseHTTPRequestHandler):
    def do_PATCH(self):
        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length) if content_length > 0 else b""
        
        print(f"Received PATCH request on path {self.path}")
        print("Authorization Header:", self.headers.get('Authorization'))
        print("Received request data:", post_data.decode('utf-8'))
        sys.stdout.flush()
        
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        
        response = {
            "status": "success",
            "message": "LIMS status updated successfully"
        }
        self.wfile.write(json.dumps(response).encode('utf-8'))

    def log_message(self, format, *args):
        # Suppress standard logging to keep test output clean
        return

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), MockLIMSHandler) as httpd:
        print(f"Mock LIMS server running on port {PORT}")
        sys.stdout.flush()
        httpd.serve_forever()

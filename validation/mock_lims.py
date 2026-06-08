#!/usr/bin/env python3
import http.server
import socketserver
import json
import sys

PORT = 28081

class MockLIMSHandler(http.server.BaseHTTPRequestHandler):
    def handle_upload(self):
        content_length = int(self.headers.get('Content-Length', 0))
        post_data = self.rfile.read(content_length) if content_length > 0 else b""
        
        print(f"Received {self.command} request on path {self.path}")
        print("Authorization Header:", self.headers.get('Authorization'))
        print("Content-Type Header:", self.headers.get('Content-Type'))
        
        # Check if multipart or binary
        ct = self.headers.get('Content-Type', '')
        if 'multipart/form-data' in ct:
            print("Detected multipart upload")
            # Print length and small portion of the data to verify structure
            print(f"Data length: {len(post_data)} bytes")
            # Print the header parts (e.g. up to first 250 characters) to show headers
            try:
                preview = post_data[:500].decode('utf-8', errors='ignore')
                print("Body Preview:\n", preview)
            except Exception as e:
                print("Could not preview body:", e)
        else:
            print(f"Detected binary upload, length: {len(post_data)} bytes")
            print("Body content (decoded):", post_data.decode('utf-8', errors='ignore'))
            
        sys.stdout.flush()
        
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        
        response = {
            "status": "success",
            "message": "File uploaded successfully"
        }
        self.wfile.write(json.dumps(response).encode('utf-8'))

    def do_POST(self):
        if self.path.endswith('/upload'):
            self.handle_upload()
        else:
            self.send_response(404)
            self.end_headers()

    def do_PUT(self):
        if self.path.endswith('/upload'):
            self.handle_upload()
        else:
            self.send_response(404)
            self.end_headers()

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

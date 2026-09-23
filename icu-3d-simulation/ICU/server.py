import os
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer


def main():
    port = int(os.environ.get("PORT", "3000"))
    server = ThreadingHTTPServer(("localhost", port), SimpleHTTPRequestHandler)
    print(f"Serving on http://localhost:{port}")
    server.serve_forever()


if __name__ == "__main__":
    main()

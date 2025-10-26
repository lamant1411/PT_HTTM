#!/usr/bin/env python3
"""
Minimal process.py used by ProcessingService (called via ProcessBuilder).
It accepts --video and --line and prints a short JSON-like result to stdout.
"""
import argparse
import time
import json

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--video', required=True)
    parser.add_argument('--line', default='')
    args = parser.parse_args()

    # Simulate work (replace with real ML inference / ultralytics usage)
    print(json.dumps({'status': 'started', 'video': args.video, 'line': args.line}))
    time.sleep(1)
    # Example result - replace with actual detection output
    result = {'status': 'done', 'violations': [], 'summary': 'no violations (demo)'}
    print(json.dumps(result))

if __name__ == '__main__':
    main()

#!/usr/bin/env python3
"""Prints the "password" object of a FilePasswords entry for a password, to put into the config.

    python3 password_hash.py                 # asks for the password
    python3 password_hash.py --sha1          # PBKDF2WithHmacSHA1, for devices older than Android 8.0

The password is read without echo and isn't stored anywhere; each run gives a new random salt.
"""
import argparse
import base64
import getpass
import hashlib
import json
import os

ITERATIONS = 120_000
HASH_BYTES = 32
SALT_BYTES = 16


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sha1", action="store_true", help="PBKDF2WithHmacSHA1 instead of PBKDF2WithHmacSHA256")
    parser.add_argument("--iterations", type=int, default=ITERATIONS)
    args = parser.parse_args()

    password = getpass.getpass("Password: ")
    if not password or password != getpass.getpass("Again: "):
        raise SystemExit("Passwords are empty or don't match")

    digest = "sha1" if args.sha1 else "sha256"
    salt = os.urandom(SALT_BYTES)
    hashed = hashlib.pbkdf2_hmac(digest, password.encode("utf-8"), salt, args.iterations, HASH_BYTES)
    print(json.dumps({
        "algorithm": "PBKDF2WithHmacSHA1" if args.sha1 else "PBKDF2WithHmacSHA256",
        "iterations": args.iterations,
        "salt": base64.b64encode(salt).decode(),
        "hash": base64.b64encode(hashed).decode(),
    }, indent=2))


if __name__ == "__main__":
    main()

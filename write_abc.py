#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Write 'ABC' to a file."""

def write_abc(filename: str = "abc_output.txt") -> None:
    """Write the string 'ABC' to the specified file."""
    with open(filename, "w", encoding="utf-8") as f:
        f.write("ABC")
    print(f"Successfully wrote 'ABC' to {filename}")


if __name__ == "__main__":
    write_abc()

#!/bin/sh
# reviewpilot Maven wrapper (simplified). The CI/build uses the system Maven;
# this script delegates to `mvn` so the wrapper file is present and functional.
exec mvn "$@"

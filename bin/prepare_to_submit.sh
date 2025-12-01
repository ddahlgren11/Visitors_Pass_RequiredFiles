#!/bin/bash
echo "Preparing to submit..."
bin/compile.sh
if [ $? -ne 0 ]; then
    echo "Compilation failed. Aborting submission."
    exit 1
fi
bin/run-tests.sh > test-results.txt
if [ $? -ne 0 ]; then
    echo "Tests failed. Aborting submission."
    exit 1
fi

echo "Creating Assignment.zip..."
zip -r Assignment.zip src test test_inputs lib README.md Dockerfile bin/compile.sh bin/run-tests.sh bin/prepare_to_submit.sh
echo "Submission ready: Assignment.zip"

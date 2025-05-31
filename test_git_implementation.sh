#!/bin/bash
# Script to test the Git implementation

# Colors for better output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Path to your Git implementation
MYGIT_PATH="java -cp $(pwd) Main"

# Auto compilation of Java implementation
javac -d . src/main/java/Main.java || {
    echo -e "${RED}❌ Compilation failed! Please check your Java code.${NC}"
    exit 1
}

# Create a clean test directory
TEST_DIR="/tmp/git-test-$(date +%s)"
mkdir -p "$TEST_DIR"
cd "$TEST_DIR"
echo -e "${GREEN}Created test directory: $TEST_DIR${NC}"

# Function to run tests
run_test() {
    local test_name="$1"
    local command="$2"
    local expected_output="$3"
    local expected_exit_code="${4:-0}"
    
    echo -e "\n${GREEN}Running test: $test_name${NC}"
    echo "Command: $command"
    
    # Execute the command and capture output and exit code
    output=$($command 2>&1)
    exit_code=$?
    
    # Check exit code
    if [ $exit_code -ne $expected_exit_code ]; then
        echo -e "${RED}❌ Test failed: Expected exit code $expected_exit_code, got $exit_code${NC}"
        return 1
    fi
    
    # Check output
    if [ -n "$expected_output" ] && [[ "$output" != *"$expected_output"* ]]; then
        echo -e "${RED}❌ Test failed: Output does not contain expected string${NC}"
        echo "Expected to contain: $expected_output"
        echo "Got: $output"
        return 1
    fi
    
    echo -e "${GREEN}✓ Test passed!${NC}"
    return 0
}

# Test init command
test_init() {
    echo -e "\n${GREEN}Testing 'init' command${NC}"
    
    run_test "Init repository" "$MYGIT_PATH init" "Initialized git directory"
    
    # Check if .git directory was created with expected structure
    if [ -d ".git" ] && [ -d ".git/objects" ] && [ -d ".git/refs" ] && [ -f ".git/HEAD" ]; then
        echo -e "${GREEN}✓ .git directory structure is correct${NC}"
    else
        echo -e "${RED}❌ .git directory structure is incorrect${NC}"
        return 1
    fi
    
    # Check HEAD content
    head_content=$(cat .git/HEAD)
    if [ "$head_content" = "ref: refs/heads/main" ] || [ "$head_content" = "ref: refs/heads/main\n" ]; then
        echo -e "${GREEN}✓ HEAD content is correct${NC}"
    else
        echo -e "${RED}❌ HEAD content is incorrect: $head_content${NC}"
        return 1
    fi
    
    return 0
}

# Test hash-object -w and cat-file -p with fixed input
test_hash_and_cat() {
    echo -e "\n${GREEN}Testing 'hash-object -w' and 'cat-file -p' commands${NC}"

    # Write fixed input string to file
    echo -n "diggity doggity donkey doo doo woop woop" > test_blob.txt

    # Write object and capture hash
    hash_output=$(${MYGIT_PATH} hash-object -w test_blob.txt)

    if [ -z "$hash_output" ] || [ ${#hash_output} -ne 40 ]; then
        echo -e "${RED}❌ Failed to get valid hash from hash-object -w${NC}"
        return 1
    fi

    echo -e "${GREEN}✓ Hash generated: $hash_output${NC}"

    # Verify object file exists
    object_dir=".git/objects/${hash_output:0:2}"
    object_file="${object_dir}/${hash_output:2}"

    if [ ! -f "$object_file" ]; then
        echo -e "${RED}❌ Object file $object_file does not exist${NC}"
        return 1
    fi

    echo -e "${GREEN}✓ Object file $object_file exists${NC}"

    # Use cat-file -p to read the content back
    cat_output=$(${MYGIT_PATH} cat-file -p "$hash_output")

    if [ "$cat_output" = "diggity doggity donkey doo doo woop woop" ]; then
        echo -e "${GREEN}✓ cat-file returned correct content${NC}"
        return 0
    else
        echo -e "${RED}❌ cat-file returned incorrect content: $cat_output${NC}"
        return 1
    fi    
}

# Main testing function
main() {
    echo -e "${GREEN}Starting Git implementation tests...${NC}"
    
    local tests_passed=0
    local tests_failed=0
    
    # Run all tests
    if test_init; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi

    if test_hash_and_cat; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Print summary
    echo -e "\n${GREEN}Tests completed!${NC}"
    echo -e "${GREEN}Tests passed: $tests_passed${NC}"
    if [ $tests_failed -gt 0 ]; then
        echo -e "${RED}Tests failed: $tests_failed${NC}"
        exit 1
    else
        echo -e "${GREEN}All tests passed successfully!${NC}"
        exit 0
    fi
}

# Run tests
main

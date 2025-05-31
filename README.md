[![progress-banner](https://backend.codecrafters.io/progress/git/9d7750f0-99a2-4948-92e2-2a20fdce4aba)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

This is a starting point for Java solutions to the
["Build Your Own Git" Challenge](https://codecrafters.io/challenges/git).

In this challenge, you'll build a small Git implementation that's capable of
initializing a repository, creating commits and cloning a public repository.
Along the way we'll learn about the `.git` directory, Git objects (blobs,
commits, trees etc.), Git's transfer protocols and more.

**Note**: If you're viewing this repo on GitHub, head over to
[codecrafters.io](https://codecrafters.io) to try the challenge.

# Passing the first stage

The entry point for your Git implementation is in `src/main/java/Main.java`.
Study and uncomment the relevant code, and push your changes to pass the first
stage:

```sh
git commit -am "pass 1st stage" # any msg
git push origin master
```

That's all!

# Stage 2 & beyond

Note: This section is for stages 2 and beyond.

1. Ensure you have `mvn` installed locally
1. Run `./your_program.sh` to run your Git implementation, which is implemented
   in `src/main/java/Main.java`.
1. Commit your changes and run `git push origin master` to submit your solution
   to CodeCrafters. Test output will be streamed to your terminal.

# Testing locally

The `your_program.sh` script is expected to operate on the `.git` folder inside
the current working directory. If you're running this inside the root of this
repository, you might end up accidentally damaging your repository's `.git`
folder.

We suggest executing `your_program.sh` in a different folder when testing
locally. For example:

```sh
mkdir -p /tmp/testing && cd /tmp/testing
/path/to/your/repo/your_program.sh init
```

To make this easier to type out, you could add a
[shell alias](https://shapeshed.com/unix-alias/):

```sh
alias mygit=/path/to/your/repo/your_program.sh

mkdir -p /tmp/testing && cd /tmp/testing
mygit init
```

# Alternative Testing
A majority of the code in this project can only be tested and submitted
using the Codecrafters CLI or Git systems if one has a paid membership.
Thus, a local testing script has been creaated to test the functionality
of the code locally, by running the command:
```sh
./test_git_implementation.sh
```

This script runs tests against all the Git subsystem commands implemented
so far (e.g. `init`, `hash-object`, `cat-file`) to help verify your implementation.

# Git Subsystem Features Implemented
This project currently implements the following Git subsystem commands:
* `git init`
  Initializes a new Git repository by creating the `.git` directory 
  along with the minimum structure required, such as:
  - `.git/` - Root directory for Git metadata
  - `.git/objects/` - Directory for all Git objects (blobs, commits, trees)
  - `.git/refs/` - Directory for storing references to branches
  - `.git/HEAD` - File pointing to the default branch (`ref: refs/heads/main)
* `git cat-file -p <hash>`
  Reads and pretty-prints the contents of a Git object to standard
  output (current supports only blob objects).   
* `git hash-object -w <file>`
  Writes a blob object to the Git object store, computing and returning
  the SHA-1 hash of the content   
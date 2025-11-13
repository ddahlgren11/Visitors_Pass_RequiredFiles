# Use the official OpenJDK image as the base image
FROM eclipse-temurin:17-jdk


# Set the maintainer label
LABEL maintainer="edwjones@ccu.edu"

# Create necessary directories
RUN mkdir -p /workspace /workspace/bin /bin /opt/junit /opt/javacc

# Install required utilities and download JUnit and JavaCC
RUN apt-get update && apt-get install -y \
    zip \
    unzip \
    wget \
    curl \
    git \
    jq \
    rsync \
    nano && \
    wget -O /opt/junit/junit-platform-console-standalone.jar https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.8.2/junit-platform-console-standalone-1.8.2.jar && \
    wget -O /opt/javacc/javacc-7.0.13.jar https://repo1.maven.org/maven2/net/java/dev/javacc/javacc/7.0.13/javacc-7.0.13.jar && \
    apt-get clean

# Set the working directory inside the container
WORKDIR /workspace

# Download ASM jars
RUN mkdir -p /opt/asm && \
    wget -O /opt/asm/asm-9.8.jar https://repo1.maven.org/maven2/org/ow2/asm/asm/9.8/asm-9.8.jar && \
    wget -O /opt/asm/asm-commons-9.8.jar https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.8/asm-commons-9.8.jar && \
    wget -O /opt/asm/asm-tree-9.8.jar https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.8/asm-tree-9.8.jar && \
    wget -O /opt/asm/asm-util-9.8.jar https://repo1.maven.org/maven2/org/ow2/asm/asm-util/9.8/asm-util-9.8.jar

# Set CLASSPATH to include JUnit, ASM jars, and current directory
ENV CLASSPATH="/opt/junit/junit-platform-console-standalone.jar:/opt/asm/asm-9.8.jar:/opt/asm/asm-commons-9.8.jar:/opt/asm/asm-tree-9.8.jar:/opt/asm/asm-util-9.8.jar:."

# Create javacc wrapper script
RUN echo '#!/bin/sh' > /usr/local/bin/javacc && \
    echo 'java -cp /opt/javacc/javacc-7.0.13.jar org.javacc.parser.Main "$@"' >> /usr/local/bin/javacc && \
    chmod +x /usr/local/bin/javacc


# Add the bin directory to the PATH
ENV PATH="/workspace/bin:${PATH}"

# Create the init.sh script
RUN echo '#!/bin/bash' > /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo 'echo "----------------------------------------------------"' >> /usr/local/bin/init.sh && \
    echo 'echo "Welcome to the CSC-385 Project Initializer!"' >> /usr/local/bin/init.sh && \
    echo 'echo "----------------------------------------------------"' >> /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo '# Clone the source repository' >> /usr/local/bin/init.sh && \
    echo 'echo "Cloning the source repository into a temporary directory..."' >> /usr/local/bin/init.sh && \
    echo 'git clone https://github.com/edwjonesga/ccu-classes.git /tmp/ccu-classes-source' >> /usr/local/bin/init.sh && \
    echo 'if [ $? -ne 0 ]; then' >> /usr/local/bin/init.sh && \
    echo '    echo "Failed to clone the source repository. Aborting."' >> /usr/local/bin/init.sh && \
    echo '    exit 1' >> /usr/local/bin/init.sh && \
    echo 'fi' >> /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo '# Copy files to the workspace' >> /usr/local/bin/init.sh && \
    echo 'echo "Copying project files into your workspace..."' >> /usr/local/bin/init.sh && \
    echo 'cp -r /tmp/ccu-classes-source/csc-385/* /workspace/' >> /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo '# Initialize a new git repository' >> /usr/local/bin/init.sh && \
    echo 'echo "Initializing a new git repository..."' >> /usr/local/bin/init.sh && \
    echo 'cd /workspace/' >> /usr/local/bin/init.sh && \
    echo 'git config --global --add safe.directory /workspace' >> /usr/local/bin/init.sh && \
    echo 'git init' >> /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo '# Commit initial files and create branches' >> /usr/local/bin/init.sh && \
    echo 'echo "Committing initial project files to main branch..."' >> /usr/local/bin/init.sh && \
    echo 'git checkout -b main' >> /usr/local/bin/init.sh && \
    echo 'git add .' >> /usr/local/bin/init.sh && \
    echo 'git config --global user.email "init@csc-385init.ccu.edu";git config --global user.name "CSC-385 Init Script"' >> /usr/local/bin/init.sh && \
    echo 'git commit -m "Initial commit of project files"' >> /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo '# Create source-update-branch from main' >> /usr/local/bin/init.sh && \
    echo 'echo "Creating source-update-branch..."' >> /usr/local/bin/init.sh && \
    echo 'git checkout -b source-update-branch' >> /usr/local/bin/init.sh && \
    echo 'git checkout main' >> /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo '# Prompt for remote repository' >> /usr/local/bin/init.sh && \
    echo 'echo "Please provide the URL for your new GitHub repository."' >> /usr/local/bin/init.sh && \
    echo 'read -p "Enter your GitHub repo URL (e.g., https://github.com/username/repo.git): " remote_url' >> /usr/local/bin/init.sh && \
    echo 'git remote add origin $remote_url' >> /usr/local/bin/init.sh && \
    echo 'echo "Remote '\''origin'\'' set to $remote_url"' >> /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo '# Clean up' >> /usr/local/bin/init.sh && \
    echo 'echo "Cleaning up temporary files..."' >> /usr/local/bin/init.sh && \
    echo 'rm -rf /tmp/ccu-classes-source' >> /usr/local/bin/init.sh && \
    echo '' >> /usr/local/bin/init.sh && \
    echo 'echo "----------------------------------------------------"' >> /usr/local/bin/init.sh && \
    echo 'echo "Initialization complete!"' >> /usr/local/bin/init.sh && \
    echo 'echo ""' >> /usr/local/bin/init.sh && \
    echo 'echo "This container will now exit."' >> /usr/local/bin/init.sh && \
    echo 'echo "Please run the same '\''docker run'\'' command again to start your development session."' >> /usr/local/bin/init.sh && \
    echo 'echo "----------------------------------------------------"' >> /usr/local/bin/init.sh && \
    echo 'exit 0' >> /usr/local/bin/init.sh

# Make the init script executable
RUN chmod +x /usr/local/bin/init.sh

# Create the entrypoint.sh script to handle runtime permissions
RUN echo '#!/bin/sh' > /usr/local/bin/entrypoint.sh && \
    echo '#' >> /usr/local/bin/entrypoint.sh && \
    echo '# This script is the entrypoint for the Docker container.' >> /usr/local/bin/entrypoint.sh && \
    echo '# It ensures that any scripts in /workspace/bin are executable.' >> /usr/local/bin/entrypoint.sh && \
    echo '#' >> /usr/local/bin/entrypoint.sh && \
    echo '' >> /usr/local/bin/entrypoint.sh && \
    echo '# Make all .sh files in /workspace/bin executable' >> /usr/local/bin/entrypoint.sh && \
    echo 'if [ -d "/workspace/bin" ]; then' >> /usr/local/bin/entrypoint.sh && \
    echo '  find /workspace/bin -type f -name "*.sh" -exec chmod +x {} +' >> /usr/local/bin/entrypoint.sh && \
    echo 'fi' >> /usr/local/bin/entrypoint.sh && \
    echo '' >> /usr/local/bin/entrypoint.sh && \
    echo '# Execute the command passed to the container (e.g., /bin/bash)' >> /usr/local/bin/entrypoint.sh && \
    echo 'exec "$@"' >> /usr/local/bin/entrypoint.sh

# Make the entrypoint script executable
RUN chmod +x /usr/local/bin/entrypoint.sh

# Set the entrypoint to our script
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

# Set the default command to be executed by the entrypoint
CMD ["/bin/bash"]
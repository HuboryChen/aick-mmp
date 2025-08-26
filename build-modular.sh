#!/bin/bash

# AICK-MMP Modular Build Script

set -e  # Exit on any error

echo "🚀 Starting AICK-MMP Modular Build Process..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed. Please install Maven 3.6+ and try again."
    exit 1
fi

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed. Please install Docker and try again."
    exit 1
fi

# Parse command line arguments
BUILD_TYPE=${1:-"all"}
SKIP_TESTS=${2:-"true"}

print_status "Build type: $BUILD_TYPE"
print_status "Skip tests: $SKIP_TESTS"

# Clean previous builds
print_status "Cleaning previous builds..."
cd backend
mvn clean
cd ..

# Build based on type
case $BUILD_TYPE in
    "shared")
        print_status "Building shared module only..."
        cd backend
        mvn install -pl aick-mmp-shared -am $([ "$SKIP_TESTS" = "true" ] && echo "-DskipTests")
        cd ..
        ;;
    "central")
        print_status "Building central services..."
        cd backend
        mvn package -pl aick-mmp-central -am $([ "$SKIP_TESTS" = "true" ] && echo "-DskipTests")
        cd ..
        ;;
    "edge")
        print_status "Building edge services..."
        cd backend
        mvn package -pl aick-mmp-edge -am $([ "$SKIP_TESTS" = "true" ] && echo "-DskipTests")
        cd ..
        ;;
    "all")
        print_status "Building all modules..."
        cd backend
        mvn package $([ "$SKIP_TESTS" = "true" ] && echo "-DskipTests")
        cd ..
        ;;
    "docker")
        print_status "Building all modules for Docker..."
        cd backend
        mvn package $([ "$SKIP_TESTS" = "true" ] && echo "-DskipTests")
        cd ..
        print_status "Building Docker images..."
        docker-compose -f docker-compose-modular.yml build
        ;;
    *)
        print_error "Invalid build type. Use: shared, central, edge, all, or docker"
        exit 1
        ;;
esac

# Check build status
if [ $? -eq 0 ]; then
    print_success "Build completed successfully!"
    
    # Show JAR sizes for comparison
    if [ "$BUILD_TYPE" = "all" ] || [ "$BUILD_TYPE" = "docker" ]; then
        print_status "JAR file sizes:"
        if [ -f "backend/aick-mmp-central/target/aick-mmp-central-0.0.1-SNAPSHOT.jar" ]; then
            CENTRAL_SIZE=$(du -h backend/aick-mmp-central/target/aick-mmp-central-*.jar | cut -f1)
            echo "  📦 Central services: $CENTRAL_SIZE"
        fi
        if [ -f "backend/aick-mmp-edge/target/aick-mmp-edge-0.0.1-SNAPSHOT.jar" ]; then
            EDGE_SIZE=$(du -h backend/aick-mmp-edge/target/aick-mmp-edge-*.jar | cut -f1)
            echo "  📦 Edge services: $EDGE_SIZE"
        fi
        if [ -f "backend/aick-mmp-shared/target/aick-mmp-shared-0.0.1-SNAPSHOT.jar" ]; then
            SHARED_SIZE=$(du -h backend/aick-mmp-shared/target/aick-mmp-shared-*.jar | cut -f1)
            echo "  📦 Shared components: $SHARED_SIZE"
        fi
    fi
    
    print_status "Build artifacts are ready for deployment!"
else
    print_error "Build failed!"
    exit 1
fi

# Show usage instructions
echo ""
print_status "Next steps:"
echo "  🔹 To run central services: docker-compose -f docker-compose-modular.yml up central-1 central-2"
echo "  🔹 To run edge nodes: docker-compose -f docker-compose-modular.yml up edge-node-1 edge-node-2"
echo "  🔹 To run full system: docker-compose -f docker-compose-modular.yml up"
echo "  🔹 To run specific JAR: java -jar backend/aick-mmp-central/target/aick-mmp-central-*.jar"
echo "  🔹 To run edge JAR: java -jar backend/aick-mmp-edge/target/aick-mmp-edge-*.jar"
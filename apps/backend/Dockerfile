FROM bellsoft/liberica-native-image-kit-container:jdk-25-nik-25-musl AS builder
WORKDIR /build

# Spring AOT (run during nativeCompile) instantiates the prod context to contribute bean
# definitions. Give it a region and disable IMDS so AWS SDK client beans build cleanly with no
# credentials/network at image-build time.
ENV AWS_REGION=us-east-1 AWS_EC2_METADATA_DISABLED=true

COPY apps/backend .
RUN ./gradlew nativeCompile


FROM bellsoft/liberica-runtime-container:jdk-24-musl
WORKDIR /app

COPY --from=builder /build/build/native/nativeCompile/solitar server
ENTRYPOINT ["/app/server"]

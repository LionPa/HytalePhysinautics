FROM messense/cargo-zigbuild@sha256:4c18634a3d9c7775683b0f1e313584770d32216623a1273a104b634fa294bf4c

ARG RUST_VERSION
RUN rustup default ${RUST_VERSION}
RUN rustup target add x86_64-apple-darwin
RUN rustup target add aarch64-apple-darwin
RUN rustup target add x86_64-unknown-linux-gnu
RUN rustup target add aarch64-unknown-linux-gnu
RUN rustup target add x86_64-unknown-freebsd

WORKDIR /

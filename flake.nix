{
  description = "A very basic flake";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-23.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = import nixpkgs { inherit system; };
          jdk = pkgs.jdk17;
        in
        {
          devShells.default = pkgs.mkShell {
            buildInputs = with pkgs; [
              jdk
              protobuf
              # customise the jdk which gradle uses by default
              (callPackage gradle-packages.gradle_8 {
                java = jdk;
              })
            ];
          };
        }
      );
}

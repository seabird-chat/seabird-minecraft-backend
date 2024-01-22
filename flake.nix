{
  description = "A very basic flake";

  inputs.nixpkgs.url = "github:nixos/nixpkgs/nixos-23.11";

  outputs = { self, nixpkgs }: {

    devShells.x86_64-linux.default = let
        pkgs = import nixpkgs { system = "x86_64-linux"; };
        # choose our preferred jdk package
        jdk = pkgs.jdk17;
    in pkgs.mkShell {
        buildInputs = with pkgs; [
          jdk
          protobuf
          # customise the jdk which gradle uses by default
          (callPackage gradle-packages.gradle_8 {
            java = jdk;
          })
        ];
    };
  };
}

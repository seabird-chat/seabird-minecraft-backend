{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";
    treefmt-nix.url = "github:numtide/treefmt-nix";
  };

  outputs =
    inputs@{
      self,
      nixpkgs,
      flake-parts,
      ...
    }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = nixpkgs.lib.systems.flakeExposed;

      imports = [
        inputs.treefmt-nix.flakeModule
      ];

      perSystem =
        {
          pkgs,
          system,
          config,
          lib,
          ...
        }:
        {
          treefmt = {
            settings.programs.nixfmt.enable = true;
            #settings.programs.google-java-format.enable = true;
            settings.global.on-ummatched = "info";
          };

          devShells.default =
            pkgs.mkShell {
              packages = with pkgs; [
                pkgs.jdk17
                pkgs.gradle_8
              ];
            };
        };
    };
}

# PlayStation Archive viewer

A viewer for PlayStation Archive (PSARC) archives.

![](https://github.com/user-attachments/assets/f1a1a042-70c9-480b-aa96-7d9e74f89697)

## Installation

1. Download the latest release from the [releases page](https://github.com/ShadelessFox/psarc/releases/latest) for your operating system
2. Unzip the downloaded archive and launch using `psarc.exe` on Windows or `bin/psarc` on Linux

#### Nightly builds

If you want to try the latest features and improvements, you can download the latest build from the [actions page](https://github.com/ShadelessFox/psarc/actions).
Click on the latest workflow run and download the artifact from the `Artifacts` section for your operating system.

## Command Line

### Listing files

All files from an archive can be listed using the `list` command:

```bash
psarc-cli list path/to/archive.psarc
```

Additional options can be specified:

- `--verbose` to make the output include offset, size, and compressed size of files within the archive,
- `--output` to specify the output file (by default, outputs to the standard output).

### Extracting files

All files from an archive can be extracted using the `extract` command:

```bash
psarc-cli extract --dir path/to/output path/to/archive.psarc
```

## License

This project is licensed under the GPL-3.0 license.

This project uses icons by [Yusuke Kamiyamane](http://p.yusukekamiyamane.com/), licensed under a [CC BY 3.0 License](http://creativecommons.org/licenses/by/3.0/).
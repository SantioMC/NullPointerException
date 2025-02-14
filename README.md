<div align="center">

# NullPointerException

Paper exploit prevention against malformed packets

</div>

> [!NOTE]
> This plugin is still under development, while heavily tested it may contain bugs or unpatched exploits.
> Please report any issues you find to the [issue tracker](https://github.com/SantioMC/NullPointerException/issues).

## Installation

Installing and configuring the plugin is extremely simple, just follow the steps below:

Download and add the plugin to your server in the `plugins` folder, once installed 
the server should be restarted to load the plugin. Once the plugin has run for
the first time you can then configure  the plugin at `plugins/NullPointerException/config.yml`.

## Configuration

At the moment, this plugin does not have a configuration file, however, it will be added in the future where
certain settings can be configured and toggled on/off.

## Alerts and Bypassing

By default, NPE will not alert staff members when a player is detected for a malformed packet, this is because
even when a malformed packet is detected, NPE may alter the packet to prevent the exploit from being executed.
However, this can easily be changed by running the command `/npe alerts` and toggling the alerts on for yourself.

To toggle bypass this plugin, you can run the command `/npe bypass` and toggle the bypass off (on by default 
for staff) for yourself and get disconnected when a malformed packet is detected. Even in bypass mode, staff will
get flagged for malformed packets but no remediation will be done.

## License

This project is licensed under the [MIT License](https://github.com/SantioMC/NullPointerException/blob/main/LICENSE). If
this project has helped you in any way, please consider [supporting me](https://github.com/sponsors/SantioMC).

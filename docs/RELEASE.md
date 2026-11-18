# Values still required before a public release

Version `1.0.0` is set. Nothing in this repository has been published, and no
project URL was supplied. Leave these blank until they are real. Do not invent
them, and do not publish from this tree.

## Required

| Value | Where it is missing |
| --- | --- |
| Homepage URL | `src/main/resources/fabric.mod.json` has no `contact` object, so no `contact.homepage`. `src/main/resources/META-INF/mods.toml` has no `displayURL`. The store drafts in `docs/store/` have no project link. |
| Source repository URL | `fabric.mod.json` has no `contact.sources`. The store drafts do not name a source repository. |
| Issues URL | `fabric.mod.json` has no `contact.issues`. `mods.toml` has no `issueTrackerURL`. |
| Modrinth project URL | `docs/store/modrinth.md` is a draft. There is no project slug or page URL. |
| CurseForge project URL | `docs/store/curseforge.md` is a draft. There is no project page URL. |
| Contact | No Discord, email, or other contact is set in `fabric.mod.json` `contact` or on the store drafts. |

`fabric.mod.json` today has authors, license, and icon only.
`mods.toml` today has authors, credits, license, `logoFile`, and
`displayTest = "IGNORE_ALL_VERSION"`.

## Optional, still unset

- Funding or donation URL
- Discord invite, if a community link is wanted separately from contact
- Changelog discussion or release-notes URL beyond `docs/store/CHANGELOG-1.0.0.md`

## Before uploading artifacts

Rebuild the four binary jars and run `python3 tools/audit_release.py` on them.
The jars audited on 2026-11-10 predate the entity-replacement lifecycle fix
and the 1.21.11 `pack.mcmeta` (`min_format` / `max_format` `[75, 0]`).

A Forge 1.21.11 dedicated server logs one pack-metadata error asking for
`supported_formats`. That key makes the 1.21.11 client reject the resource
pack, so it is not set. The dedicated-server proof still syncs emotes. See
[STATUS.md](STATUS.md).

Lifecycle proof (respawn, dimension change, disconnect/reconnect, F3+T) was
run on Fabric 1.21.11 and Forge 1.21.1. It was not repeated on Fabric 1.21.1
or Forge 1.21.11.

Focused sword-salute stills after the framing change exist for Fabric 1.21.11.
The other three loader/version targets were not recaptured.

Store frames for a gallery are in `docs/store/shots/` (1920×1080) and
`docs/store/icon-512.png`.

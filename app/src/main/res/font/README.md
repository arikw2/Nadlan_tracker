# Bundled fonts

Both families are licensed under the **SIL Open Font License 1.1** and carry full
Hebrew coverage. They are bundled rather than loaded as downloadable fonts so the
first frame is never wrong (no Play Services dependency, no async fallback flash).

| Files | Family | Upstream |
|---|---|---|
| `rubik_medium.ttf`, `rubik_semibold.ttf`, `rubik_bold.ttf` | Rubik — display voice | [google/fonts `ofl/rubik`](https://github.com/google/fonts/tree/main/ofl/rubik) |
| `assistant_light.ttf`, `assistant_regular.ttf`, `assistant_semibold.ttf`, `assistant_bold.ttf` | Assistant — body voice | [google/fonts `ofl/assistant`](https://github.com/google/fonts/tree/main/ofl/assistant) |

Upstream ships each family as a single variable font. These static instances were
produced with `fontTools.varLib.instancer` at the weights the theme declares —
Rubik at 500/600/700, Assistant at 300/400/600/700 — which keeps the APK smaller
than shipping two variable fonts and matches the `FontWeight` values in `Type.kt`.

To regenerate:

```bash
pip install fonttools
curl -sSLO https://raw.githubusercontent.com/google/fonts/main/ofl/rubik/Rubik%5Bwght%5D.ttf
python3 -m fontTools.varLib.instancer 'Rubik[wght].ttf' wght=600 -o rubik_semibold.ttf
```

Filenames must stay lowercase with underscores, or aapt rejects them.

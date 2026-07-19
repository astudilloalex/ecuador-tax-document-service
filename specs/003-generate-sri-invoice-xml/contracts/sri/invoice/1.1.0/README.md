# SRI Invoice 1.1.0 Validation Contract

This directory preserves the exact offline schema graph approved for Feature 003 planning. Runtime
resources must copy these bytes unchanged into `src/main/resources/sri/invoice/1.1.0/`. The
implementation-owned `catalog.xml` closes resolution locally; it is not an SRI artifact and may be
changed only when tests continue to prove strict, network-free resolution of the pinned graph.

| Resource | Authority and retrieval date | Bytes | SHA-256 |
|----------|------------------------------|------:|--------|
| `factura_V1.1.0.xsd` | SRI Invoice XML/XSD ZIP, retrieved 2026-07-19 | 36,356 | `62db9bf0ecceb00ef2b7ed136e59224815e5e5e33c77efc6a0552001e052eb8b` |
| `xmldsig-core-schema.xsd` | W3C XML Signature Recommendation dated 2002-02-12, retrieved 2026-07-19 | 10,293 | `35cf8197da812c85e40d57891b35c94187569ed474a2dac813ce5090dafcd35c` |
| `XMLSchema.dtd` | W3C, retrieved 2026-07-19 | 16,075 | `2032ead9fd47a61b22fe56aa02be1840bd9bb9015b0c0d3f1e8aac75dd91c3b9` |
| `datatypes.dtd` | W3C, retrieved 2026-07-19 | 6,357 | `6946432ca7af2e9584f91b48564111fd2c73c8debbbcd9a0e3f5ddd382eeb51c` |

The containing SRI ZIP was 24,515 bytes with SHA-256
`ba1ff0c4e329fe759c3f88dc75f2975780b315b6eb3d0069071b77c1f26fec03`. Its source is
<https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/05546998-6f29-4870-be3b-62650f312a6c/XML%20y%20XSD%20Factura.zip>.

The W3C sources are:

- <https://www.w3.org/TR/2002/REC-xmldsig-core-20020212/xmldsig-core-schema.xsd>
- <https://www.w3.org/2001/XMLSchema.dtd>
- <https://www.w3.org/2001/datatypes.dtd>

Validation must set secure processing, prohibit external DTD and schema access, use catalog
resolution in strict mode, and fail with `INVOICE_XML_VALIDATOR_UNAVAILABLE` when a resource,
mapping, byte length, or digest differs. No runtime network or filesystem fallback is allowed.

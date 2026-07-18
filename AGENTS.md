## Postman integration

When interacting with Postman resources:

- Always use the Postman MCP server named `postman`.
- Target workspace ID: `f4829cb9-b05d-4e1f-b6cb-d30f4c4191ec`.
- Target collection UID: `15834347-0a294a6f-98e2-4ad1-8262-c209ff9ccd4c`.
- Target collection name: `ecuador-tax-document-service`.
- The API source of truth is: `/src/main/resources/META-INF`.
- Never create another workspace or collection when the target already exists.
- Never update a collection selected only by name; verify its UID first.
- Preserve existing authentication, variables, scripts, examples, descriptions,
  folder structure, and request identifiers unless a change is explicitly required.
- Never delete obsolete requests automatically. Report them separately.
- Before performing writes, present the proposed changes.
- After performing writes, retrieve the collection again and verify the result.
# Source merge notes

The user-supplied Java snippets were merged into the latest PeoClient source where their intended changes could be identified without replacing complete classes with placeholder/ellipsis content.

Applied functional changes:
- DiagnosticRecorder: added getPendingSize().
- PeoClient NukerLogic: added a per-tick duplicate-position guard and a conservative 10-block client-side processing cap.
- Existing full NukerLogic implementation, recovery state, diagnostics, and Fabric 1.21.4 mappings were preserved.

Important: several uploaded snippets contained comments such as `// ... code cũ ...`, `// ... existing code ...`, or `// ... Các phương thức khác giữ nguyên ...`. Those fragments were not used as wholesale file replacements because doing so would destroy the complete source already present in the latest client.

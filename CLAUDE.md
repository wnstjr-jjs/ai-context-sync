# AI Context Sync — CLAUDE.md

## Project Overview
Subscription SaaS that automatically syncs project context across AI tools.
Users switch between Claude, ChatGPT, Gemini freely — context always follows them.
No more manually uploading markdown files before each session.

## Core Value
- Context persists even when switching AI tools
- Teams share the same context regardless of which AI they use
- Developers use MCP/CLI commit flow; non-developers use Chrome extension + web UI

## Business Model
Subscription SaaS.

| Plan | Price | Key Limit |
|---|---|---|
| Free | $0 | 3 projects, 7-day history |
| Pro | $5–8/mo | Unlimited projects + history |
| Team | $10–15/user/mo | Pro + team sharing + member invite |

Upgrade trigger: natural upsell when user exceeds 3 projects.

## Architecture

    [Single Backend Server]
           |
           +-- Chrome Extension  -->  Claude.ai / ChatGPT / Gemini users
           |
           +-- MCP Server        -->  Claude Code / Cursor / Claude Desktop users

## Tech Stack
- Backend: Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, PostgreSQL 16
- File storage: PostgreSQL TEXT for MVP, S3 in phase 2
- Chrome Extension: Vanilla JS
- MCP Server: Python or Node.js (calls backend API)
- Python: PDF/PPT text extraction, AI summarization

## Target Users

| Target | Client | Plan |
|---|---|---|
| General users | Chrome extension | Free |
| Solo developer | MCP | Free |
| Dev team | MCP + team sharing | Paid |
| Non-dev team | Chrome extension + team sharing | Paid |

## Database Schema

    users          (id, email, password, plan, created_at)
    projects       (id, user_id, name, description, created_at)
    contexts       (id, project_id, content TEXT, source, label, version, created_at)
                   -- content is AES-256 encrypted
    project_members(project_id, user_id, role, invited_at)

    -- Phase 2 addition (no changes to existing tables):
    context_files  (id, project_id, file_name, file_type, s3_key, size_bytes, uploaded_at)

## API Design

### Auth
    POST /api/auth/signup
    POST /api/auth/login       returns JWT
    POST /api/auth/refresh

### Projects
    GET    /api/projects
    POST   /api/projects
    DELETE /api/projects/{id}

### Context (core)
    POST   /api/projects/{id}/context           save context (commit or web upload)
    GET    /api/projects/{id}/context/latest    pulled by extension / MCP
    GET    /api/projects/{id}/context/history   version history
    DELETE /api/projects/{id}/context/{cid}     delete specific version

### File Upload (non-developers)
    POST   /api/projects/{id}/context/upload    upload file -> extract text + return AI draft
    POST   /api/projects/{id}/context/confirm   user confirms draft -> save

### Team (paid)
    POST   /api/projects/{id}/members
    DELETE /api/projects/{id}/members/{uid}
    GET    /api/projects/{id}/members

### Context save request example
    POST /api/projects/{id}/context
    {
      "content": "today's session summary...",
      "source": "claude.ai",
      "label": "auth module design complete"
    }

### Context pull response example (phase 1)
    GET /api/projects/{id}/context/latest
    {
      "projectName": "My Project",
      "content": "...",
      "savedAt": "2026-05-04T10:00:00",
      "version": 5
    }

### File upload response example (AI draft)
    POST /api/projects/{id}/context/upload
    {
      "draftContent": "AI-generated context summary...",
      "sourceFile": "architecture.pdf",
      "extractedAt": "2026-05-04T10:00:00"
    }

## MCP Tools
    get_context     ->  GET  /api/projects/{id}/context/latest
    save_context    ->  POST /api/projects/{id}/context
    list_projects   ->  GET  /api/projects

## Chrome Extension Structure

    extension/
    +-- manifest.json
    +-- background.js        API communication, token management
    +-- content/
    |   +-- claude.js        Claude.ai DOM injection (MVP)
    |   +-- chatgpt.js       phase 2
    |   +-- gemini.js        phase 2
    +-- popup/
        +-- popup.html       project selector UI
        +-- popup.js

### Context injection approach
Each AI platform: inject context into the prompt input field at conversation start.

- Claude.ai: insert into contenteditable div, fire React synthetic InputEvent
- ChatGPT: same pattern
- Gemini: same pattern
- Claude Code / Cursor: MCP get_context call

### Injected prefix format

    Below is the current project context.
    ---
    {content pulled from server}
    ---
    Please respond based on this context.

### Extension maintenance notes
- Claude.ai UI updates can break DOM selectors — build selector change detection tests early
- React synthetic event trigger is the most common breakage point after UI updates

## Context Save Flow

### Developer flow (MCP/CLI)
1. Working in Claude Code or Cursor
2. Run commit command
3. MCP sends context to backend

### Non-developer flow (web UI)
1. Upload PDF/PPT via web UI drag-and-drop
2. Server extracts text (Python)
3. AI generates summary draft
4. User reviews and edits draft in web UI — this review screen is the core UX
5. User confirms -> saved to DB

## Security
- AES-256 encryption on context content, per-user keys via AWS KMS
- Backend open-sourced on GitHub for trust
- Self-hosting option for sensitive users
- Access log transparency (planned)
- Privacy policy: operator has no access to context content (legally binding)

## Development Order (MVP)
1. Spring Boot API — auth, project CRUD, context save/fetch
2. Chrome Extension — login, new conversation detection, auto pull + inject (Claude.ai first)
3. MCP Server — Claude Code / Cursor integration, commit command
4. Web UI — project management, file upload + AI summary review screen, team invite (paid)

## Local Dev Environment
- OS: WSL2 (Ubuntu)
- Java: 21 (OpenJDK)
- DB: PostgreSQL 16 via Docker Compose
  - host: localhost:5432
  - database: aicontext
  - user: dev
  - password: devpassword
- Docker Compose file: ~/ai-context-sync/docker/docker-compose.yml

## Current Status
- Architecture and design complete
- Business model confirmed (subscription SaaS)
- API design complete (including file upload + AI summary flow)
- Security design complete
- Development not yet started — setting up local environment now
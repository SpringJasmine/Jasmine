# Jasmine

A study app that connects to your Notion, Google Drive, and Dropbox — parses documents using Unstructured — and helps you learn from your own notes, papers, and files.

## Features

- **Multi-source ingestion** — Pull study material from Notion pages, Google Drive folders, and Dropbox directories
- **Document parsing** — Unstructured extracts text, tables, and images from PDFs, DOCX, PPTX, and other formats
- **Usage-based billing** — Stripe-powered metered billing: pay per document processed, with configurable monthly limits
- **Smart study tools** — Flashcards, summaries, and Q&A generated from your parsed content

## Architecture

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Notion     │  │ Google Drive │  │   Dropbox    │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └────────┬────────┴────────┬────────┘
                │                 │
         ┌──────▼───────┐  ┌─────▼──────┐
         │ Unstructured  │  │   Stripe    │
         │  (parsing)    │  │  (billing)  │
         └──────┬────────┘  └─────┬──────┘
                │                 │
           ┌────▼─────────────────▼────┐
           │       Jasmine Core        │
           │  (study tools + limits)   │
           └───────────────────────────┘
```

## Billing

Jasmine uses **usage-based billing** via Stripe:

- Each document parse operation is a metered event
- Users set a monthly spending limit (e.g. $20/mo)
- When the limit is reached, Stripe stops charging and Unstructured stops processing new files
- Usage resets at the start of each billing cycle

### Configuration

| Variable | Description |
|----------|-------------|
| `STRIPE_SECRET_KEY` | Stripe API secret key |
| `STRIPE_PRICE_ID` | Metered price ID for per-document billing |
| `STRIPE_WEBHOOK_SECRET` | Webhook endpoint secret for usage events |
| `MONTHLY_LIMIT_CENTS` | Default spending cap in cents (default: 2000) |
| `UNSTRUCTURED_API_KEY` | Unstructured.io API key |
| `UNSTRUCTURED_API_URL` | Unstructured endpoint URL |

## Quick Start

```bash
# Install dependencies
uv sync

# Set up environment
cp .env.example .env
# Fill in your API keys

# Run
uv run jasmine serve
```

## Development

```bash
uv sync --group dev
uv run pytest
uv run ruff check .
```

# Oxford proxy

Legacy optional sample only. The current recommended production path for this project is to avoid this service entirely and use the app's free dictionary providers instead.

This small Node service keeps Oxford credentials off the Android client if you later choose a paid dictionary vendor.

Expected API:

- `GET /validate?word=apple`
- Response: `{ "word": "apple", "valid": true, "source": "Oxford Dictionaries" }`

Recommended deployment targets:

- Google Cloud Run
- Firebase Functions v2 with an Express adapter

Set `OXFORD_APP_ID` and `OXFORD_APP_KEY` as server environment variables before deploying.

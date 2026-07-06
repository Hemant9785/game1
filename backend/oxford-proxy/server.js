import cors from "cors";
import express from "express";

const app = express();
app.use(cors());

app.get("/validate", async (req, res) => {
  const word = String(req.query.word || "").trim().toLowerCase();

  if (!word || !/^[a-z-]+$/.test(word)) {
    return res.status(400).json({
      word,
      valid: false,
      source: "Oxford Dictionaries",
      message: "Submit a single alphabetic word."
    });
  }

  const appId = process.env.OXFORD_APP_ID;
  const appKey = process.env.OXFORD_APP_KEY;

  if (!appId || !appKey) {
    return res.status(500).json({
      word,
      valid: false,
      source: "Oxford Dictionaries",
      message: "Server credentials are missing."
    });
  }

  const oxfordUrl = `https://od-api.oxforddictionaries.com/api/v2/entries/en-us/${encodeURIComponent(word)}`;

  try {
    const response = await fetch(oxfordUrl, {
      headers: {
        app_id: appId,
        app_key: appKey
      }
    });

    if (response.ok) {
      return res.json({
        word,
        valid: true,
        source: "Oxford Dictionaries"
      });
    }

    if (response.status === 404) {
      return res.json({
        word,
        valid: false,
        source: "Oxford Dictionaries",
        message: "Word not found in Oxford Dictionaries."
      });
    }

    const responseText = await response.text();
    return res.status(502).json({
      word,
      valid: false,
      source: "Oxford Dictionaries",
      message: `Oxford upstream error ${response.status}: ${responseText.slice(0, 180)}`
    });
  } catch (error) {
    return res.status(502).json({
      word,
      valid: false,
      source: "Oxford Dictionaries",
      message: error instanceof Error ? error.message : "Unknown proxy error."
    });
  }
});

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

const port = Number(process.env.PORT || 8080);
app.listen(port, () => {
  console.log(`Word Duel Oxford proxy listening on ${port}`);
});

# How Presigned URL Signing Works (AWS SigV4)

## The core primitive: HMAC

HMAC stands for Hash-based Message Authentication Code.

```
HMAC(key, message) → fixed-length digest
```

Two properties matter here:

1. **One-way** — given the digest, you cannot recover the key or the message
2. **Key-dependent** — the same message produces a completely different digest with a different key

This means: if a service sees a digest it can reproduce using its copy of a secret key — it knows the signature was made by someone who had that key. Without the key you cannot forge a valid digest, even if you know the exact message that was signed.

---

## The two credentials

- **Access key ID** — like a username. Included in requests and URLs. Not secret. Its job is to tell the server which secret key to look up for verification.
- **Secret access key** — like a password. Never transmitted. Stored on both the signing side and the verifying side. This is what drives the HMAC.

---

## What gets signed

The signer doesn't just sign the URL path. It signs a **canonical request** — a precise, normalized string that encodes the full intent of the operation:

```
PUT
/user_abc/f47ac10b/report.pdf
X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...&X-Amz-Expires=900
content-length:2048576
content-type:application/pdf
host:my-bucket.account.r2.cloudflarestorage.com

content-length;content-type;host
UNSIGNED-PAYLOAD
```

Everything that matters is in here: the HTTP method, the exact path, the query parameters, the required headers, the expiry. If any of it changes, the signature is invalid.

---

## The signing process (SigV4)

SigV4 does not sign with the raw secret key directly. It derives a **signing key** by chaining four rounds of HMAC:

```
signingKey = HMAC(
               HMAC(
                 HMAC(
                   HMAC("AWS4" + secretKey, date),    // date key
                 region),                              // region key
               service),                              // service key
             "aws4_request")                          // request key
```

Expanded:

```
dateKey    = HMAC("AWS4" + secretKey, "20260418")
regionKey  = HMAC(dateKey, "auto")
serviceKey = HMAC(regionKey, "s3")
signingKey = HMAC(serviceKey, "aws4_request")
```

Then the actual signature:

```
signature = HMAC(signingKey, stringToSign)
```

Where `stringToSign` is:

```
AWS4-HMAC-SHA256
20260418T154958Z                        ← timestamp
20260418/auto/s3/aws4_request           ← scope
SHA256(canonicalRequest)                ← hash of what we built above
```

---

## What appears in the URL

```
?X-Amz-Algorithm=AWS4-HMAC-SHA256
&X-Amz-Date=20260418T154958Z
&X-Amz-Expires=900
&X-Amz-Credential=<accessKeyId>/20260418/auto/s3/aws4_request
&X-Amz-SignedHeaders=content-length;content-type;host
&X-Amz-Signature=bc945f7d70b67fd43c...
```

- `accessKeyId` — tells the server which secret key to look up. Not secret.
- `X-Amz-Signature` — the HMAC digest. Proves the URL was made by someone with the secret key.
- The secret access key — **nowhere in the URL, ever.**

---

## How the server verifies it

When a client hits a presigned URL:

1. Server reads `X-Amz-Credential` to find the access key ID
2. Looks up the corresponding **secret access key** in its own storage
3. Reconstructs the exact same canonical request from the incoming HTTP request
4. Runs the same SigV4 derivation to compute what the signature *should* be
5. Compares that to `X-Amz-Signature` in the URL

If they match — authorized. If the path changed, a header is missing, the expiry has passed, or the signature was tampered with — rejected.

The secret key never moves at request time. Both sides have had a copy since the credentials were created. Same inputs → same HMAC output → verified.

---

## Why the key derivation chain matters

The four-step chain (date → region → service → "aws4_request") means:

- A signing key derived for `20260418/auto/s3` cannot sign requests for a different date or service
- Even if a signing key was somehow extracted from memory on a specific day, it would be useless the next day
- The secret key is never used directly in signing, making it harder to extract from intermediate material

---

## Why this is better than a random token approach

A naive approach: generate a random token, store it in a database, validate it on every request.

Problems:
- The server is in the hot path for every operation
- Token expiry and revocation need to be managed manually
- The database takes the load for every file transfer

With presigned URLs: the cryptographic proof is self-contained in the URL. The server can verify it with zero external lookups. The expiry is baked into the signed string and cannot be extended without the secret key.

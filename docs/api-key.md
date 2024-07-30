## API Key Management

For accesing the Mushu API, you need a proper API key that needs to be generated. For this, follow these steps:

1. Open a terminal and generate a new key with `openssl rand -hex 32`
2. Create a file under `/api/config/config.ts` with the following format:

```
export const Config = {
    API_KEY: '<YOUR_KEY_HERE>',
}
```

3. Paste the key generated in step 1 in that file

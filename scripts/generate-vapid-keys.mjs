#!/usr/bin/env node
/**
 * Generates the VAPID key pair used by Web Push.
 *
 * The public key is the uncompressed P-256 point (0x04 || x || y) encoded as URL-safe
 * base64, which is what browsers expect in `applicationServerKey`. The private key is the
 * raw `d` scalar. Put both values in `.env` (never commit the private key).
 */
import { generateKeyPairSync } from 'node:crypto';

const { publicKey, privateKey } = generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
const publicJwk = publicKey.export({ format: 'jwk' });
const privateJwk = privateKey.export({ format: 'jwk' });

const uncompressedPublicKey = Buffer.concat([
  Buffer.from([0x04]),
  Buffer.from(publicJwk.x, 'base64url'),
  Buffer.from(publicJwk.y, 'base64url')
]);

console.log(`VAPID_PUBLIC_KEY=${uncompressedPublicKey.toString('base64url')}`);
console.log(`VAPID_PRIVATE_KEY=${Buffer.from(privateJwk.d, 'base64url').toString('base64url')}`);
console.log('VAPID_SUBJECT=mailto:tu-correo@dominio.com');

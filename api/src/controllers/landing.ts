import { Request, Response } from 'express';
import path from 'path';

export class LandingController {
    constructor() {

      }
    public renderHomePage(): string {
        return path.join(__dirname, '../views/landing.html')
    }
}

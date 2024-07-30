import { Request, Response } from 'express'

export const handleJsonPost = (req: Request, res: Response) => {
    // Access the parsed JSON body
    const reqBody = req.body

    if (!reqBody) {
        res.status(400).json({
            message: `No request body!`,
        })
    }

    res.status(200).json({
        message: 'Success',
        data: { dummy1: 'param1' },
    })
}

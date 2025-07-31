import * as vscode from "vscode"
import { LoginIn } from "./wecode-util"

const qrcodeUrl = "https://qrcode.sina.com.cn/qrcode/newdata?appid=1130" // 获取二维码
const qrcodeStatusUrl = "https://qrcode.sina.com.cn/qrcode/status?appid=1130&poll=1&sid=" //获取二维码状态
const qrcodeExpireTime = 120 //查询状态超时时间
const qrcodeInterval = 1000 //查询间隔1s
let lastQrcoeSid = ""

export async function getQrcodeUrl(): Promise<any> {
	let result = {
		loginUrl: "",
		qrcodeSid: "",
	}

	try {
		const response = await fetch(qrcodeUrl, {
			method: "GET",
			headers: {
				Accept: "application/json",
			},
		})

		if (!response.ok) {
			throw new Error(`HTTP error! status: ${response.status}`)
		}
		const text = await response.text()
		const jsonData = JSON.parse(text)
		if (jsonData.status === 0) {
			result.loginUrl = jsonData.data.data
			result.qrcodeSid = jsonData.data.sid
		}
	} catch (error) {
		console.error("Error fetching data:", error)
		vscode.window.showErrorMessage(`Error get qrcode error : ${error}`)
	}

	if (result.loginUrl === "" || result.qrcodeSid === "") {
		vscode.window.showWarningMessage("Login failed, please try again.")
	}

	return result
}

export async function getQrcodeStatus(sid: string): Promise<string> {
	lastQrcoeSid = sid
	let username = ""
	try {
		for (let i = 0; i < qrcodeExpireTime; i++) {
			if (sid !== lastQrcoeSid) {
				break
			}
			const response = await fetch(qrcodeStatusUrl + sid, {
				method: "GET",
				headers: {
					Accept: "application/json",
				},
			})

			if (!response.ok) {
				throw new Error(`HTTP error! status: ${response.status}`)
			}
			const text = await response.text()
			const jsonData = JSON.parse(text)
			if (jsonData.status === 2) {
				username = jsonData.data.username
				LoginIn(username)
				break
			}
			if (username === "") {
				await sleep(qrcodeInterval)
			}
		}
	} catch (error) {
		console.error("Error qrcode status query : ", error)
	}
	return username
}

export function sleep(milliseconds: number) {
	return new Promise((r) => setTimeout(r, milliseconds))
}

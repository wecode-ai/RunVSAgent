// 导入必要的模块
import * as os from "os"
import * as path from "path"
import fs from "fs-extra"
import toml from "toml"
import { stringify } from "@iarna/toml"
import * as vscode from "vscode"
import { WECODE_PROXY_URL } from "../../api/providers/wecoder_constants"

const homeDir = os.homedir()
const configFilepath = path.join(homeDir, ".wecode-client", "agent", "config.toml")
if (!fs.existsSync(configFilepath)) {
	fs.ensureFileSync(configFilepath) // 创建文件
}
let loginStatus: boolean = false
let loginUserName: string = ""
let endpoint: string = ""

;(function initConfig() {
	updateLoginStatus()
	watchConfigFile(() => {})
	watchWboxLogin()
})()

async function watchWboxLogin() {
	if (vscode.env.appName.startsWith("WBoxStudio") === true) {
		let wboxUser = await getwboxUser()
		if (wboxUser && wboxUser.trim().length > 0) {
			LoginInHandler(wboxUser, false)
		}
		vscode.authentication.onDidChangeSessions(async (e) => {
			if (e.provider.id === "WBOX") {
				let wboxUser = await getwboxUser()
				if (wboxUser && wboxUser.trim().length > 0) {
					LoginInHandler(wboxUser, false)
				} else {
					LoginOutHandler(false)
				}
			}
		})
	}
}

async function getwboxUser() {
	let wboxUser: string = ""
	const userCredentials: any = await vscode.authentication.getSession("WBOX", ["wbox"], {
		silent: true,
		createIfNone: false,
	})
	wboxUser = userCredentials?.erpAccount?.id || ""
	return wboxUser
}

export function watchConfigFile(doneCallback: () => any): void {
	fs.watch(configFilepath, (_eventType: any, filename: any) => {
		if (filename) {
			updateLoginStatus()
			doneCallback()
		}
	})
}

export function sleep(milliseconds: number) {
	return new Promise((r) => setTimeout(r, milliseconds))
}

export function getLoginStatus(): boolean {
	return loginStatus
}
export function getLoginWecodeUser(): string {
	return loginUserName
}

export function getEndpoint(): string {
	return endpoint
}

export function LoginInHandler(loginUser: string, _notice: boolean): void {
	loginUserName = loginUser
	loginStatus = true
	const config = readConfig()
	if (!config.server) {
		config.server = {}
	}
	if (!config.server.requestHeaders) {
		config.server.requestHeaders = {}
	}
	config.server.requestHeaders.wecodeUser = loginUser
	config.server.requestHeaders.wecodeSession = "1"
	writeConfig(config)
	// if (notice) {
	//     vscode.window.showInformationMessage("WeCode: 登录成功！");
	// }
}

export function LoginOutHandler(_notice: boolean): void {
	loginUserName = ""
	loginStatus = false
	const config = readConfig()
	config.server.requestHeaders.wecodeUser = ""
	config.server.requestHeaders.wecodeSession = ""
	writeConfig(config)
	// if (notice) {
	//     vscode.window.showInformationMessage("WeCode: 退出登录成功！");
	// }
}

export function LoginIn(loginUser: string): void {
	LoginInHandler(loginUser, true)
}
export function LoginOut(): void {
	LoginOutHandler(true)
}

function readConfig(): any {
	try {
		const content = fs.readFileSync(configFilepath, "utf8")
		return toml.parse(content)
	} catch (error) {
		console.error("Failed to read config file:", error)
		return {}
	}
}

function writeConfig(config: any): void {
	try {
		const content = stringify(config)
		fs.writeFileSync(configFilepath, content)
	} catch (error) {
		console.error("Failed to write config file:", error)
	}
}

export function updateLoginStatus() {
	const config = readConfig()
	const wecodeUser = config.server?.requestHeaders?.wecodeUser
	const wecodeSession = config.server?.requestHeaders?.wecodeSession
	if (wecodeUser && wecodeUser.trim().length > 0 && wecodeSession && wecodeSession.trim().length > 0) {
		loginStatus = true
		loginUserName = wecodeUser
	} else {
		loginStatus = false
		loginUserName = ""
	}
	endpoint = config.server?.endpoint
}

export function getWordStartIndices(text: string): number[] {
	const indices: number[] = []
	const re = /\b\w/g
	let match
	while ((match = re.exec(text)) !== null) {
		indices.push(match.index)
	}
	return indices
}

type Platform = "mac" | "linux" | "windows" | "unknown"

export function getPlatform(): Platform {
	const platform = os.platform()
	if (platform === "darwin") {
		return "mac"
	} else if (platform === "linux") {
		return "linux"
	} else if (platform === "win32") {
		return "windows"
	} else {
		return "unknown"
	}
}

export function getAltOrOption() {
	if (getPlatform() === "mac") {
		return "⌥"
	} else {
		return "Alt"
	}
}

export function getMetaKeyLabel() {
	const platform = getPlatform()
	switch (platform) {
		case "mac":
			return "⌘"
		case "linux":
		case "windows":
			return "^"
		default:
			return "^"
	}
}

export function getMetaKeyName() {
	const platform = getPlatform()
	switch (platform) {
		case "mac":
			return "Cmd"
		case "linux":
		case "windows":
			return "Ctrl"
		default:
			return "Ctrl"
	}
}

// 从 ConfigManager 中复制而来
export function generateId(): string {
	return Math.random().toString(36).substring(2, 15)
}

// Interface for Claude quota response
export interface modelQuotaResponse {
	data: {
		quota: number
		remaining: number
		usage: number
		user: string
		user_quota_detail?: {
			monthly_quota: number
			monthly_usage: number
			permanent_quota: number
			permanent_usage: number
		}
	}
	status: string
}

export async function getModelQuota(username: string, provider: string): Promise<modelQuotaResponse | null> {
	try {
		const response = await fetch(WECODE_PROXY_URL + `/v1/${provider}/quota`, {
			headers: {
				"wecode-user": username,
			},
		})

		if (!response.ok) {
			console.error(`Failed to fetch ${provider} quota:`, response.statusText)
			return null
		}

		const data = await response.json()
		return data as modelQuotaResponse
	} catch (error) {
		console.error(`Error fetching ${provider} quota:`, error)
		return null
	}
}

import * as vscode from "vscode"
import { getLoginWecodeUser } from "./wecode-util"

export function generateHeaders(command: string) {
	const version = vscode.extensions.getExtension("weiboplat.wecoder")?.packageJSON?.version
	const headers: Record<string, string> = {}
	headers["wecode-action"] = command.replace("wecode.", "")
	headers["wecode-user"] = getLoginWecodeUser()
	headers["wecode-client"] = vscode.env.appName + " " + vscode.version
	headers["wecode-ide-name"] = vscode.env.appName
	headers["wecode-ide-version"] = vscode.version
	headers["wecode-plugin-name"] = "wecoder"
	headers["wecode-plugin-version"] = version
	headers["wecode-source"] = "agent"
	return headers
}

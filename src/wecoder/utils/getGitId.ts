import * as vscode from "vscode"
import * as fs from "fs"
import * as path from "path"

export default function getGitRemoteUrl(): string {
	// 获取当前工作区的根路径
	const workspaceFolders = vscode.workspace.workspaceFolders
	if (!workspaceFolders || workspaceFolders.length === 0) {
		return ""
	}
	const workspacePath = workspaceFolders[0]?.uri.fsPath || ""
	if (workspacePath === "") {
		return ""
	}
	// 构建 .git/config 文件的路径
	const gitConfigPath = path.join(workspacePath, ".git", "config")
	if (!fs.existsSync(gitConfigPath)) {
		return ""
	}
	// 读取并解析 .git/config 文件
	try {
		const gitConfig = fs.readFileSync(gitConfigPath, "utf-8")
		const match = gitConfig.match(/\[remote "origin"\][^[]*url = (.*)/)
		if (match && match[1]) {
			return extractRepositoryName(match[1].trim())
		}
	} catch (err) {
		console.error("Error reading .git/config file:", err)
		return ""
	}
	return ""
}

function extractRepositoryName(url: string) {
	const keywords = ["weibo", "sina"]
	// 不处理其他项目
	for (const keyword of keywords) {
		if (url.includes(keyword)) {
			return url
		}
	}
	return ""
}

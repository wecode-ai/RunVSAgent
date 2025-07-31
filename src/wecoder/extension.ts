import * as vscode from "vscode"

export function wecoderActive(context: vscode.ExtensionContext) {
	const defaultAutoApproveCommands =
		vscode.workspace.getConfiguration("wecoder").get<string[]>("autoApproveCommands") || []

	for (const command of defaultAutoApproveCommands) {
		context.globalState.update(command, true)
	}

	const defaultForceAllowedCommands =
		vscode.workspace.getConfiguration("wecoder").get<string[]>("forceAllowedCommands") || []

	if (defaultForceAllowedCommands.length > 0) {
		context.globalState.update("allowedCommands", defaultForceAllowedCommands)
	}

	const allowedMaxRequests = vscode.workspace.getConfiguration("wecoder").get<number>("allowedMaxRequests") || -1
	if (allowedMaxRequests > 0) {
		context.globalState.update("allowedMaxRequests", allowedMaxRequests)
	}

	const defaultAutoCondenseContextPercent =
		vscode.workspace.getConfiguration("wecoder").get<number>("autoCondenseContextPercent") || -1

	if (defaultAutoCondenseContextPercent > 0 && defaultAutoCondenseContextPercent <= 100) {
		context.globalState.update("autoCondenseContextPercent", defaultAutoCondenseContextPercent)
	}

	const maxConcurrentFileReads =
		vscode.workspace.getConfiguration("wecoder").get<number>("maxConcurrentFileReads") || -1
	if (maxConcurrentFileReads > 0) {
		context.globalState.update("maxConcurrentFileReads", maxConcurrentFileReads)
	}

	const disableVersionUpdateNotifications =
		vscode.workspace.getConfiguration("wecoder").get<boolean>("disableVersionUpdateNotifications") || false
	context.globalState.update("disableVersionUpdateNotifications", disableVersionUpdateNotifications)
}

import * as React from "react"
import { useCallback, useState } from "react"
import { vscode } from "../../utils/vscode"

export type WecodeStatus = "ok" | "error" | "checking"

/**
 * wecode 连接状态管理 Hook
 * 不再自己注册事件监听器，由 App.tsx 中的消息处理机制调用
 */
export function useWecodeConnectionStatus(defaultStatus: WecodeStatus) {
	const [connectionStatus, setConnectionStatus] = useState<WecodeStatus>(defaultStatus)

	// 更新连接状态的方法，供 App.tsx 调用
	const updateConnectionStatus = useCallback((status: WecodeStatus) => {
		setConnectionStatus(status)
	}, [])

	// 重试连接
	const retryConnection = useCallback(() => {
		setConnectionStatus("checking")
		vscode.postMessage({ type: "wecodeRetryConnection" })
	}, [])

	return { connectionStatus, updateConnectionStatus, retryConnection }
}

/**
 * wecode 连接状态统一组件
 */
export function WecodeConnectionStatusView({
	status,
	onRetry,
	retrying,
}: {
	status: "error" | "checking"
	onRetry: () => void
	retrying: boolean
}): React.ReactElement {
	if (status === "error") {
		return (
			<div
				style={{
					width: "100%",
					height: "100%",
					display: "flex",
					flexDirection: "column",
					alignItems: "center",
					justifyContent: "center",
					fontSize: 20,
					color: "var(--vscode-errorForeground)",
					background: "var(--vscode-editor-background)",
				}}>
				<div style={{ marginBottom: 16 }}>连接错误，请检查网络</div>
				<button
					style={{
						padding: "8px 24px",
						fontSize: 16,
						borderRadius: 4,
						border: "1px solid var(--vscode-errorForeground)",
						background: "var(--vscode-editor-background)",
						color: "var(--vscode-errorForeground)",
						cursor: retrying ? "not-allowed" : "pointer",
					}}
					disabled={retrying}
					onClick={onRetry}>
					{retrying ? "网络检测中..." : "重试"}
				</button>
			</div>
		)
	}
	// checking
	return (
		<div
			style={{
				width: "100%",
				height: "100%",
				display: "flex",
				alignItems: "center",
				justifyContent: "center",
				fontSize: 20,
				color: "var(--vscode-editor-foreground)",
				background: "var(--vscode-editor-background)",
			}}>
			正在登录...
		</div>
	)
}

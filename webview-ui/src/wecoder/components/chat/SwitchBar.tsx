import { vscode } from "@/utils/vscode"

const WeCodeSwitchBar = () => {
	return (
		<div>
			<div
				style={{
					backgroundColor: "var(--vscode-editor-background)",
					padding: "4px 16px",
					textAlign: "center",
					borderBottom: "1px solid var(--vscode-widget-border)",
					color: "var(--vscode-editor-foreground)",
					cursor: "pointer",
					lineHeight: "20px",
				}}
				onClick={() => {
					vscode.postMessage({ type: "openWecodeChat" })
				}}>
				<span style={{ textDecoration: "underline" }}>🤖 Code Agent 模式 | 点击切换到普通模式</span>
			</div>
		</div>
	)
}
export default WeCodeSwitchBar

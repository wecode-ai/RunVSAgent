import { vscode } from "../../utils/vscode"
import { Mode } from "../../../../src/shared/modes"

interface ModelSelectorProps {
	currentAiModelSource: string | undefined
	mode: Mode
	aiModeType: Array<{ name: string }> | undefined
	currentApiConfigName: string | undefined
	disabled: boolean
}

const ModelSelector = ({
	currentAiModelSource,
	aiModeType,
	currentApiConfigName,
	disabled,
	mode,
}: ModelSelectorProps) => {
	return (
		<div
			style={{
				display: "flex",
				alignItems: "center",
			}}>
			<span
				style={{
					color: "var(--vscode-foreground)",
					flexShrink: 0,
					paddingLeft: "2em",
				}}>
				<h4>
					一键使用大模型
					<a
						href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/wecoder/model-desc"
						target="_blank"
						rel="noopener noreferrer"
						style={{
							marginLeft: "5px",
							fontSize: "0.8em",
							color: "var(--vscode-textLink-foreground)",
						}}>
						<span
							className="codicon codicon-unverified"
							aria-label="帮助文档"
							style={{
								display: "inline-flex",
								alignItems: "center",
								verticalAlign: "middle",
								position: "relative",
								top: "-2px",
							}}></span>
					</a>
				</h4>
			</span>
			<div
				style={{
					display: "flex",
					flexDirection: "column",
					gap: "4px",
					marginLeft: "8px",
				}}>
				{(aiModeType || []).map((config) => (
					<label
						key={config.name}
						style={{
							display: "flex",
							alignItems: "center",
							gap: "4px",
							cursor: disabled ? "not-allowed" : "pointer",
							opacity: disabled ? 0.5 : 1,
						}}>
						<input
							type="radio"
							name="aiModelSource"
							value={config.name}
							checked={
								currentAiModelSource === config.name ||
								(currentAiModelSource === "auto" && config.name === "auto")
							}
							disabled={disabled}
							onChange={(e) => {
								const value = e.target.value
								if (value === "settings-action") {
									window.postMessage({ type: "action", action: "settingsButtonClicked" })
									return
								}
								vscode.postMessage({
									type: "loadApiConfiguration",
									text: currentApiConfigName,
									aiModelSource: value,
									mode: mode,
								})
							}}
							style={{
								accentColor: "var(--vscode-button-background)",
								cursor: disabled ? "not-allowed" : "pointer",
							}}
						/>
						<span style={{ color: "var(--vscode-foreground)" }}>{config.name}</span>
					</label>
				))}
			</div>
		</div>
	)
}

export default ModelSelector

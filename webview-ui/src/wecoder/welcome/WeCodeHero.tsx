import { useState } from "react"
import { useExtensionState } from "@/context/ExtensionStateContext"
import ModelSelector from "@/components/chat/ModelSelector"
import { WECODE_STARTUP_NAME, WECODE_STARTUP_URL } from "@src/wecoder/config/config"
import "./WeCodeHero.css"

const WeCodeHero = () => {
	// const [imagesBaseUri] = useState(() => {
	// 	const w = window as any
	// 	return w.IMAGES_BASE_URI || ""
	// })
	const { currentApiConfigName, currentAiModelSoure } = useExtensionState()
	const { mode } = useExtensionState()
	const [textAreaDisabled] = useState(false)
	const [toolsExpanded, setToolsExpanded] = useState(false)
	return (
		<div className="flex flex-col items-left justify-center pb-4">
			<h1>欢迎使用WeCode Agent</h1>
			<ModelSelector
				currentAiModelSource={currentAiModelSoure}
				aiModeType={[{ name: "内网模型" }, { name: "公网国内模型(⚠️)" }, { name: "公网海外模型(⚠️)" }]}
				mode={mode}
				currentApiConfigName={currentApiConfigName}
				disabled={textAreaDisabled}
			/>
			<p>
				阅读{" "}
				<a
					href="https://alidocs.dingtalk.com/i/nodes/X6GRezwJlLp46NY4u53dR0AyJdqbropQ"
					style={{ color: "var(--vscode-textLink-foreground)" }}>
					WeCode Agent最佳实践文档
				</a>{" "}
				提升使用效率, 查看{" "}
				<a
					href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/wecoder/help"
					style={{ color: "var(--vscode-textLink-foreground)" }}>
					帮助文档
				</a>{" "}
				了解更多详情， 或者加入{" "}
				<a
					href="https://qr.dingtalk.com/action/joingroup?code=v1,k1,s8zIEYd6GCOuoFaf1hSO4qxA+FSgubnJzwaUIsjRXho=&_dt_no_comment=1&origin=11? axb邀请你加入钉钉群聊WeCode代码助手，点击进入查看详情"
					style={{ color: "var(--vscode-textLink-foreground)" }}>
					微博WeCode钉钉群
				</a>{" "}
				参与讨论。
				<br />
				<br />
				<span style={{ display: "inline-flex", alignItems: "center", marginRight: 4, marginBottom: 3 }}>
					• WeCode Agent已接入内网Kimi K2模型，欢迎测试
					<span style={{ marginLeft: 4 }}>
						<svg width="28" height="14" viewBox="0 0 28 14" fill="none" xmlns="http://www.w3.org/2000/svg">
							<rect width="28" height="14" rx="4" fill="#FF4D4F" />
							<text
								x="14"
								y="10"
								textAnchor="middle"
								fill="#fff"
								fontSize="10"
								fontWeight="bold"
								fontFamily="Arial">
								NEW
							</text>
						</svg>
					</span>
				</span>
				<br style={{ margin: "5px 0" }} />
				<span style={{ display: "inline-flex", alignItems: "center", marginRight: 4, marginBottom: 3 }}>
					• {"WeCode Agent"}
					<a
						href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/plugin/idea"
						style={{ color: "var(--vscode-textLink-foreground)" }}>
						Jetbrains版
					</a>
					及
					<a
						href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/plugin/xcode"
						style={{ color: "var(--vscode-textLink-foreground)" }}>
						XCode版
					</a>
					已正式发布
				</span>
				<br style={{ margin: "5px 0" }} />
				<span style={{ display: "inline-flex", alignItems: "center", marginRight: 4 }}>• 参与</span>
				<a href={WECODE_STARTUP_URL} style={{ color: "var(--vscode-textLink-foreground)" }}>
					{WECODE_STARTUP_NAME}
				</a>{" "}
				获取更多模型使用配额。
			</p>

			<hr
				style={{
					margin: "0px 0px 10px 0px",
					border: "none",
					borderTop: "1px solid var(--vscode-editorWidget-border)",
				}}
			/>

			<div style={{ padding: "0 5px", flexShrink: 0 }}>
				<div
					onClick={() => setToolsExpanded(!toolsExpanded)}
					style={{
						cursor: "pointer",
						display: "flex",
						alignItems: "center",
						userSelect: "none",
					}}>
					<p
						style={{
							margin: 0,
							fontWeight: 500,
							fontSize: "1.05em",
							color: "var(--vscode-editor-foreground)",
							borderBottom: "1px dotted var(--vscode-textLink-activeForeground)",
							paddingBottom: "2px",
						}}>
						<span
							className="codicon codicon-rocket"
							style={{
								marginRight: 6,
								color: "var(--vscode-textLink-activeForeground)",
								fontSize: "1.1em",
								verticalAlign: "middle",
							}}
						/>
						点击这里获取更多AI提效工具:
					</p>
					<span
						style={{
							marginLeft: "5px",
							transform: toolsExpanded ? "rotate(90deg)" : "rotate(0deg)",
							transition: "transform 0.3s ease",
							fontSize: "12px",
							fontWeight: "bold",
						}}>
						▶
					</span>
				</div>

				{toolsExpanded && (
					<div className="wecode-tools-container">
						<div className="wecode-section">
							<h2 className="wecode-section-title">WeCode AI辅助开发平台</h2>
							<p className="wecode-section-desc">
								全面提升开发效率的智能编码平台，集成本地、云端及多IDE插件，助力开发者轻松拥抱AI。
							</p>

							<ul className="wecode-product-list">
								<li className="wecode-product-item">
									<a
										href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/wecode_ide/wecode_ide"
										className="wecode-link">
										<strong>WeCode IDE</strong>
									</a>
									<span className="wecode-product-desc">
										开箱即用的AI开发环境，支持代码补全、智能问答、编程智能体等多种AI能力。
									</span>
								</li>

								<li className="wecode-product-item">
									<a
										href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/cloud/cloud_ide"
										className="wecode-link">
										<strong>WeCode Cloud IDE</strong>
									</a>
									<span className="wecode-product-desc">
										基于云端的AI IDE，功能与本地版一致，内置8核16G Linux沙箱，随时随地开启智能开发。
									</span>
								</li>

								<li className="wecode-product-item">
									<a
										href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/plugin/home"
										className="wecode-link">
										<strong>WeCode IDE插件</strong>
									</a>
									<span className="wecode-product-desc">
										支持多款主流IDE，轻松集成WeCode AI能力。
									</span>
									<div className="wecode-plugin-links">
										<a
											href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/plugin/vscode"
											className="wecode-link wecode-sub-link">
											VS Code 插件
										</a>
										<a
											href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/plugin/idea"
											className="wecode-link wecode-sub-link">
											JetBrains 插件
										</a>
										<a
											href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/plugin/xcode"
											className="wecode-link wecode-sub-link">
											XCode 插件
										</a>
									</div>
								</li>

								<li className="wecode-product-item">
									<a
										href="https://wiki.api.weibo.com/zh/weibo_rd/dev/wecode/agent/wecoder_agent"
										className="wecode-link">
										<strong>WeCode Bot</strong>
									</a>
									<span className="wecode-product-desc">
										基于WeCode的AI离线编码助手，支持GitLab代码自动Review、闲时生成单元测试等自动化功能。
									</span>
								</li>

								<li className="wecode-product-item">
									<a
										href="https://space.intra.weibo.com/develop/code-analysis"
										className="wecode-link">
										<strong>WeCode Analysis</strong>
									</a>
									<span className="wecode-product-desc">
										AI辅助开发统计与分析平台，帮助开发者量化AI使用情况，提升个人效率。
									</span>
								</li>
							</ul>
						</div>

						<div className="wecode-section">
							<h2 className="wecode-section-title">AIGC应用引擎</h2>
							<p className="wecode-section-desc">微博大模型应用落地平台，助力业务快速接入智能应用。</p>

							<ul className="wecode-product-list">
								<li className="wecode-product-item">
									<a href="https://aigc.intra.weibo.com" className="wecode-link">
										<strong>大模型工作流应用平台</strong>
									</a>
									<span className="wecode-product-desc">
										通过可视化拖拽快速搭建AI流程，内置RAG、分类、工具调用等常用能力，一键生成API与对话页面。
									</span>
								</li>

								<li className="wecode-product-item">
									<a href="https://ai.intra.weibo.com/deer-flow/chat" className="wecode-link">
										<strong>大模型Agent应用平台（建设中）</strong>
									</a>
									<span className="wecode-product-desc">
										快速创建可自主思考、工具调用的AI Agent，助力业务自动化与智能化。
									</span>
								</li>

								<li className="wecode-product-item">
									<a href="http://admin.aigc.intra.weibo.com/home" className="wecode-link">
										<strong>MCP市场</strong>
									</a>
									<span className="wecode-product-desc">
										集成主流MCP工具及内部自研MCP服务，支持一站式发布、部署与集成。
									</span>
								</li>
							</ul>
						</div>
					</div>
				)}
			</div>
		</div>
	)
}

export default WeCodeHero

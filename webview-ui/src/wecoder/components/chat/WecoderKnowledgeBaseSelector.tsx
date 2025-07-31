import React, { useEffect, useState } from "react"
import {
	MultiSelectDropdown,
	MultiDropdownOptionType,
	MultiDropdownOption,
} from "@/wecoder/components/ui/multi-select-dropdown"
import { useAppTranslation } from "@/i18n/TranslationContext"
import { vscode } from "@/utils/vscode"
import { TooltipProvider, Tooltip, TooltipTrigger, TooltipContent } from "@/components/ui/tooltip"

const WecoderKnowledgeBaseSelector: React.FC = () => {
	const { t } = useAppTranslation()
	const [internalKnowledgeBases, setInternalKnowledgeBases] = useState<string[]>([])
	const [knowledgeBaseOptions, setKnowledgeBaseOptions] = useState<MultiDropdownOption[]>([])
	const [showDropdown, setShowDropdown] = useState(false)
	const dropdownRef = React.useRef<HTMLDivElement>(null)
	const msdTriggerRef = React.useRef<HTMLButtonElement>(null)

	useEffect(() => {
		const messageHandler = (event: MessageEvent) => {
			const message = event.data
			if (message.type === "getKnowledgeBases") {
				if (message.values && Array.isArray(message.values)) {
					setInternalKnowledgeBases(message.values)
				}
			} else if (message.type === "getKnowledgeBaseOptions") {
				if (message.options && Array.isArray(message.options)) {
					setKnowledgeBaseOptions(message.options)
				}
			}
		}
		window.addEventListener("message", messageHandler)
		return () => window.removeEventListener("message", messageHandler)
	}, [])

	useEffect(() => {
		vscode.postMessage({ type: "getKnowledgeBases" })
		vscode.postMessage({ type: "getKnowledgeBaseOptions" })
	}, [])

	// 点击外部关闭下拉（排除 Radix Popover 弹窗内容）
	useEffect(() => {
		if (!showDropdown) return
		const handleClick = (e: MouseEvent) => {
			const target = e.target as Node
			const inDropdown = dropdownRef.current && dropdownRef.current.contains(target)
			// 检查所有 Radix Popover 弹窗
			let inPopover = false
			if (target instanceof Element) {
				const popovers = document.querySelectorAll("[data-radix-popper-content-wrapper]")
				popovers.forEach((popover) => {
					if (popover.contains(target)) {
						inPopover = true
					}
				})
			}
			if (!inDropdown && !inPopover) {
				setShowDropdown(false)
			}
		}
		document.addEventListener("mousedown", handleClick)
		return () => document.removeEventListener("mousedown", handleClick)
	}, [showDropdown])

	// 自动触发 MultiSelectDropdown 的下拉弹窗
	useEffect(() => {
		if (showDropdown && msdTriggerRef.current) {
			// 微任务触发，确保组件已挂载
			setTimeout(() => {
				msdTriggerRef.current?.click()
			}, 0)
		}
	}, [showDropdown])

	// 选中数量
	const selectedCount = internalKnowledgeBases.length

	// 选中知识库 label
	const selectedLabels = knowledgeBaseOptions
		.filter((opt) => internalKnowledgeBases.includes(opt.value))
		.map((opt) => opt.label)
	// 折叠按钮内容（只显示icon+数字+箭头，未选中时极简无边框）
	const collapsedButton = (
		<TooltipProvider delayDuration={80}>
			<Tooltip>
				<TooltipTrigger asChild>
					<button
						className={
							selectedCount > 0
								? "flex items-center gap-1 px-2 py-1 rounded-md border border-vscode-dropdown-border bg-vscode-dropdown-background hover:bg-vscode-dropdown-background/80 transition-all text-xs text-vscode-dropdown-foreground cursor-pointer select-none"
								: "flex items-center gap-1 px-2 py-1 rounded-md bg-transparent border-0 shadow-none transition-all text-xs text-vscode-dropdown-foreground/60 cursor-pointer select-none"
						}
						style={{ minWidth: 0 }}
						onClick={() => setShowDropdown(true)}
						type="button">
						<i className="codicon codicon-book text-base opacity-80" />
						<span className={selectedCount > 0 ? "font-semibold text-sm" : "font-normal text-sm"}>
							{selectedCount}
						</span>
						<i className="codicon codicon-chevron-right text-base opacity-80" />
					</button>
				</TooltipTrigger>
				<TooltipContent side="top" align="center">
					{selectedLabels.length > 0 ? (
						<div className="whitespace-pre leading-snug">
							<div>知识库：</div>
							{selectedLabels.map((label, idx) => (
								<div key={label + idx}>{label}</div>
							))}
						</div>
					) : (
						<div>知识库：未选中知识库</div>
					)}
				</TooltipContent>
			</Tooltip>
		</TooltipProvider>
	)

	return (
		<div className="inline-flex items-center ml-1" ref={dropdownRef} style={{ minWidth: 0, minHeight: 0 }}>
			{!showDropdown ? (
				collapsedButton
			) : (
				<MultiSelectDropdown
					values={internalKnowledgeBases}
					title={t("chat:selectKnowledgeBase") || "选择知识库"}
					placeholder={"知识库：无"}
					options={
						knowledgeBaseOptions.length > 0
							? knowledgeBaseOptions
							: [
									{
										value: "mini-program",
										label: "微博小程序文档",
										type: MultiDropdownOptionType.ITEM,
									},
									{
										value: "plat-api",
										label: "平台api文档",
										type: MultiDropdownOptionType.ITEM,
									},
								]
					}
					onChange={(values) => {
						setInternalKnowledgeBases(values)
						vscode.postMessage({
							type: "updateKnowledgeBases",
							values: values,
						})
					}}
					triggerClassName="w-full text-ellipsis overflow-hidden"
					// 通过 ref 让弹窗自动弹出
					ref={msdTriggerRef}
				/>
			)}
		</div>
	)
}

export default WecoderKnowledgeBaseSelector

import React, { useState, useCallback, useEffect, useRef } from "react"
import { formatLargeNumber } from "@/utils/format"
import { vscode } from "@/utils/vscode"
import { ModelQuotaData } from "../../../../../src/wecoder/shared/wecoder"
import { WECODE_STARTUP_URL, WECODE_STARTUP_NAME } from "@src/wecoder/config/config"

type ModelType = "claude" | "gemini" | "openai_gpt"

interface ModelQuotaProps {
	modelType: ModelType
	showProgressBar?: boolean
	isEnabled: boolean
}

export const useModelQuota = (modelType: ModelType, isEnabled: boolean) => {
	const [modelQuota, setModelQuota] = useState<ModelQuotaData | null>(null)
	const [lastQuotaFetchTime, setLastQuotaFetchTime] = useState<number>(0)

	const fetchModelQuota = useCallback(() => {
		if (isEnabled) {
			const currentTime = Date.now()
			const timeSinceLastFetch = currentTime - lastQuotaFetchTime

			// Only fetch if at least 10 seconds have passed since last fetch
			if (timeSinceLastFetch > 10000) {
				vscode.postMessage({ type: "getModelQuota", text: modelType })
				setLastQuotaFetchTime(currentTime)
			}
		}
	}, [isEnabled, lastQuotaFetchTime, modelType])

	useEffect(() => {
		if (isEnabled) {
			// Initial quota fetch on load
			fetchModelQuota()

			// Set up listener for quota responses and state changes
			const messageListener = (event: MessageEvent) => {
				const message = event.data

				// Handle quota response
				if (message.type === "modelQuotaResponse" && message.modelQuotaData) {
					setModelQuota(message.modelQuotaData)
				}

				// Listen for state updates and request quota after state changes
				if (message.type === "state") {
					// Use setTimeout to ensure UI updates before requesting quota
					const randomDelay = Math.floor(Math.random() * 2500) + 500
					setTimeout(() => {
						fetchModelQuota()
					}, randomDelay)
				}
			}

			window.addEventListener("message", messageListener)
			return () => window.removeEventListener("message", messageListener)
		}
	}, [isEnabled, fetchModelQuota])

	return { modelQuota, fetchModelQuota }
}

// 可复用的Tooltip组件
const QuotaTooltip = ({ children, modelQuota }: { children: React.ReactNode; modelQuota?: ModelQuotaData | null }) => {
	const [showTooltip, setShowTooltip] = useState(false)
	const [hideTimeoutId, setHideTimeoutId] = useState<NodeJS.Timeout | null>(null)
	const [tooltipPosition, setTooltipPosition] = useState({ top: true, left: true })

	const triggerRef = useRef<HTMLSpanElement>(null)
	const tooltipRef = useRef<HTMLDivElement>(null)

	// 计算tooltip的最佳位置
	const calculatePosition = useCallback(() => {
		if (!triggerRef.current || !tooltipRef.current) return

		// 获取触发元素的位置信息
		const triggerRect = triggerRef.current.getBoundingClientRect()

		// 获取视口尺寸
		const viewportWidth = document.documentElement.clientWidth
		const viewportHeight = document.documentElement.clientHeight

		// 获取tooltip的尺寸（固定宽度为w-95，约为380px）
		const tooltipWidth = 380 // w-95类的近似宽度
		const tooltipHeight = tooltipRef.current.offsetHeight

		// 计算各个方向的可用空间
		const spaceAbove = triggerRect.top
		const spaceBelow = viewportHeight - triggerRect.bottom

		// 确定最佳位置
		let showOnTop = false
		let showOnLeft = false

		// 垂直位置：优先显示在下方，如果下方空间不足且上方空间足够，则显示在上方
		if (spaceBelow < tooltipHeight && spaceAbove > tooltipHeight) {
			showOnTop = true
		}

		// 水平位置：优先显示在左侧对齐，如果会超出右边界，则右对齐
		// 检查左对齐是否会超出右边界
		if (triggerRect.left + tooltipWidth > viewportWidth) {
			// 如果右对齐可行（tooltip宽度小于触发元素到左边界的距离），则使用右对齐
			if (tooltipWidth <= triggerRect.right) {
				showOnLeft = true
			}
			// 如果右对齐也不可行，则尝试居中对齐
			else if (viewportWidth >= tooltipWidth) {
				// 计算居中位置，确保不超出左右边界
				const centerPos = Math.max(0, (viewportWidth - tooltipWidth) / 2)
				// 更新triggerRect.left以实现居中效果
				Object.defineProperty(triggerRect, "left", { value: centerPos })
				showOnLeft = false
			}
		}

		setTooltipPosition({
			top: !showOnTop,
			left: !showOnLeft,
		})
	}, [])

	const handleMouseEnter = () => {
		if (hideTimeoutId) {
			clearTimeout(hideTimeoutId)
			setHideTimeoutId(null)
		}
		setShowTooltip(true)
		// 使用RAF确保DOM已更新
		requestAnimationFrame(() => {
			requestAnimationFrame(calculatePosition)
		})
	}

	const handleMouseLeave = () => {
		const timeoutId = setTimeout(() => {
			setShowTooltip(false)
		}, 200)
		setHideTimeoutId(timeoutId)
	}

	const handleTooltipMouseEnter = () => {
		if (hideTimeoutId) {
			clearTimeout(hideTimeoutId)
			setHideTimeoutId(null)
		}
	}

	const handleTooltipMouseLeave = () => {
		const timeoutId = setTimeout(() => {
			setShowTooltip(false)
		}, 200)
		setHideTimeoutId(timeoutId)
	}

	// 添加全局点击事件监听器，点击外部时关闭tooltip
	useEffect(() => {
		if (!showTooltip) return

		const handleClickOutside = (event: MouseEvent) => {
			if (
				triggerRef.current &&
				tooltipRef.current &&
				!triggerRef.current.contains(event.target as Node) &&
				!tooltipRef.current.contains(event.target as Node)
			) {
				setShowTooltip(false)
			}
		}

		document.addEventListener("click", handleClickOutside)

		// 监听窗口大小变化，重新计算位置
		window.addEventListener("resize", calculatePosition)

		// 监听滚动事件
		window.addEventListener("scroll", calculatePosition, true)

		// 使用MutationObserver监听DOM变化
		const observer = new MutationObserver(calculatePosition)
		if (tooltipRef.current) {
			observer.observe(tooltipRef.current, {
				attributes: true,
				childList: true,
				subtree: true,
			})
		}

		// 确保tooltip显示后立即计算位置
		calculatePosition()

		return () => {
			document.removeEventListener("click", handleClickOutside)
			window.removeEventListener("resize", calculatePosition)
			window.removeEventListener("scroll", calculatePosition, true)
			observer.disconnect()
		}
	}, [showTooltip, calculatePosition])

	// 组件卸载时清理定时器
	useEffect(() => {
		return () => {
			if (hideTimeoutId) {
				clearTimeout(hideTimeoutId)
			}
		}
	}, [hideTimeoutId])

	return (
		<div className="relative">
			<span
				ref={triggerRef}
				className="cursor-help"
				onMouseEnter={handleMouseEnter}
				onMouseLeave={handleMouseLeave}>
				{children}
			</span>
			{showTooltip && (
				<div
					ref={tooltipRef}
					className={`fixed z-10 p-3 bg-[var(--vscode-editor-background)] border border-[var(--vscode-widget-border)] rounded shadow-md text-sm w-95`}
					style={{
						top: tooltipPosition.top
							? triggerRef.current
								? triggerRef.current.getBoundingClientRect().bottom + 5
								: 0
							: "auto",
						bottom: !tooltipPosition.top
							? triggerRef.current
								? window.innerHeight - triggerRef.current.getBoundingClientRect().top + 5
								: 0
							: "auto",
						left: tooltipPosition.left
							? triggerRef.current
								? triggerRef.current.getBoundingClientRect().left
								: 0
							: "auto",
						right: !tooltipPosition.left
							? triggerRef.current
								? window.innerWidth - triggerRef.current.getBoundingClientRect().right
								: 0
							: "auto",
					}}
					onMouseEnter={handleTooltipMouseEnter}
					onMouseLeave={handleTooltipMouseLeave}>
					<div className="space-y-1">
						<div>
							你的当月额度为 {modelQuota?.user_quota_detail?.monthly_quota || 0}, 已经使用{" "}
							{modelQuota?.user_quota_detail?.monthly_usage || 0}, 剩余{" "}
							{(modelQuota?.user_quota_detail?.monthly_quota || 0) -
								(modelQuota?.user_quota_detail?.monthly_usage || 0)}
							。
						</div>
						<div>
							永久额度为 {modelQuota?.user_quota_detail?.permanent_quota || 0}, 已经使用{" "}
							{modelQuota?.user_quota_detail?.permanent_usage || 0}, 剩余{" "}
							{(modelQuota?.user_quota_detail?.permanent_quota || 0) -
								(modelQuota?.user_quota_detail?.permanent_usage || 0)}
							。
						</div>
						<div>当月额度每月月初重置，永久额度可参与活动获取。</div>
						<div>
							永久额度当前活动：
							<a
								href={WECODE_STARTUP_URL}
								target="_blank"
								rel="noopener noreferrer"
								className="text-[var(--vscode-textLink-foreground)] hover:underline">
								{WECODE_STARTUP_NAME}
							</a>
							。
						</div>
					</div>
				</div>
			)}
		</div>
	)
}

export const QuotaProgressBar = ({
	quota,
	usage,
	showProgressBar = true,
	modelQuota,
}: {
	quota: number
	usage: number
	showProgressBar?: boolean
	modelQuota?: ModelQuotaData | null
}) => {
	// Calculate values based on a new format if available, otherwise use a legacy format
	const totalUsage = modelQuota?.user_quota_detail ? modelQuota.user_quota_detail.monthly_usage : usage

	const totalQuota = modelQuota?.user_quota_detail ? modelQuota.user_quota_detail.monthly_quota : quota

	const usagePercentage = (totalUsage / totalQuota) * 100

	if (showProgressBar) {
		return (
			<>
				<div className="flex items-center gap-1 flex-shrink-0 px-2">
					<span className="font-bold">配额:</span>
					<QuotaTooltip modelQuota={modelQuota}>
						<span className="inline-flex items-center justify-center w-4 h-4 text-xs rounded-full border border-[var(--vscode-badge-foreground)] text-[var(--vscode-badge-foreground)] cursor-help">
							?
						</span>
					</QuotaTooltip>
				</div>
				<div className="flex items-center gap-2 flex-1 whitespace-nowrap px-2">
					<QuotaTooltip modelQuota={modelQuota}>
						<div>{formatLargeNumber(totalUsage)}</div>
					</QuotaTooltip>
					<div className="flex items-center gap-[3px] flex-1">
						<div className="flex items-center h-1 rounded-[2px] overflow-hidden w-full bg-[color-mix(in_srgb,var(--vscode-foreground)_20%,transparent)]">
							<div
								className={`h-full w-full bg-[var(--vscode-foreground)] transition-width duration-300 ease-out`}
								style={{
									width: `${usagePercentage}%`,
									transition: "width 0.3s ease-out",
								}}
							/>
						</div>
					</div>
					<QuotaTooltip modelQuota={modelQuota}>
						<div>
							{modelQuota?.user_quota_detail
								? `${formatLargeNumber(modelQuota.user_quota_detail.monthly_quota)} (+${formatLargeNumber(modelQuota.user_quota_detail.permanent_quota - modelQuota.user_quota_detail.permanent_usage)})`
								: formatLargeNumber(totalQuota)}
						</div>
					</QuotaTooltip>
				</div>
			</>
		)
	} else {
		return (
			<>
				<QuotaTooltip modelQuota={modelQuota}>
					<div>
						{formatLargeNumber(totalUsage)} /{" "}
						{modelQuota?.user_quota_detail
							? `${formatLargeNumber(modelQuota.user_quota_detail.monthly_quota)} (+${formatLargeNumber(modelQuota.user_quota_detail.permanent_quota - modelQuota.user_quota_detail.permanent_usage)})`
							: formatLargeNumber(totalQuota)}
					</div>
				</QuotaTooltip>
			</>
		)
	}
}

export const ModelQuota: React.FC<ModelQuotaProps> = ({ modelType, isEnabled, showProgressBar = true }) => {
	const { modelQuota } = useModelQuota(modelType, isEnabled)

	if (!isEnabled || !modelQuota) return null
	return (
		<QuotaProgressBar
			quota={modelQuota.quota}
			usage={modelQuota.usage}
			showProgressBar={showProgressBar}
			modelQuota={modelQuota}
		/>
	)
}

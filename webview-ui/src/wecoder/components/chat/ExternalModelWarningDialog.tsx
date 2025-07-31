import React, { useState, useEffect } from "react"
import { AlertTriangle } from "lucide-react"
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle,
} from "@/components/ui"
import { vscode } from "@/utils/vscode"

interface ExternalModelWarningDialogProps {
	open: boolean
	onOpenChange: (open: boolean) => void
	apiConfigId: string | null
	onConfirm?: () => void
	onCancel?: () => void
}

export const ExternalModelWarningDialog: React.FC<ExternalModelWarningDialogProps> = ({
	open,
	onOpenChange,
	apiConfigId,
	onConfirm,
	onCancel,
}) => {
	const handleConfirm = () => {
		// 不在这里发送消息，而是通过onConfirm回调函数处理
		if (apiConfigId) {
			vscode.postMessage({
				type: "loadApiConfigurationById",
				text: apiConfigId,
				userExternalRiskNoticeConfirmed: true, // 添加标记表示用户已确认
			})
		}

		if (onConfirm) {
			onConfirm()
		}
		onOpenChange(false)
	}

	const handleCancel = () => {
		if (onCancel) {
			onCancel()
		}
		onOpenChange(false)
	}

	return (
		<AlertDialog open={open} onOpenChange={onOpenChange}>
			<AlertDialogContent>
				<AlertDialogHeader>
					<AlertDialogTitle>
						<AlertTriangle className="text-yellow-500" />
						<span>使用公网模型使用风险提示</span>
					</AlertDialogTitle>
					<AlertDialogDescription style={{ fontSize: "14px" }}>
						您正在切换到公网模型。请注意，使用此类模型可能存在数据安全风险。确认是否继续？
					</AlertDialogDescription>
				</AlertDialogHeader>
				<AlertDialogFooter>
					<AlertDialogCancel onClick={handleCancel}>取消</AlertDialogCancel>
					<AlertDialogAction onClick={handleConfirm}>确认</AlertDialogAction>
				</AlertDialogFooter>
			</AlertDialogContent>
		</AlertDialog>
	)
}

// 添加一个全局状态管理的钩子，用于在应用中任何地方显示GPT风险提示
export const useGptWarningDialog = () => {
	const [isOpen, setIsOpen] = useState(false)
	const [pendingApiConfigId, setPendingApiConfigId] = useState<string | null>(null)

	// 监听来自后端的消息，显示外部模型风险提示
	useEffect(() => {
		const handleMessage = (event: MessageEvent) => {
			const message = event.data

			if (message.type === "externalRiskNoticeUpdate") {
				if (!message.values) return

				if (!isOpen && message.values.gptWarning && message.values.pendingApiConfigId) {
					setPendingApiConfigId(message.values.pendingApiConfigId)
					setIsOpen(true)
				}
			}
		}

		window.addEventListener("message", handleMessage)
		return () => window.removeEventListener("message", handleMessage)
	}, [isOpen])

	// 处理确认操作
	const handleConfirm = () => {
		if (pendingApiConfigId) {
			vscode.postMessage({
				type: "loadApiConfigurationById",
				text: pendingApiConfigId,
				userExternalRiskNoticeConfirmed: true, // 添加标记表示用户已确认
			})
			setPendingApiConfigId(null)
			setIsOpen(false)
		}
	}

	// 处理取消操作
	const handleCancel = () => {
		setPendingApiConfigId(null)
		setIsOpen(false)
	}

	return {
		isOpen,
		setIsOpen,
		pendingApiConfigId,
		setPendingApiConfigId,
		handleConfirm,
		handleCancel,
	}
}

export default ExternalModelWarningDialog

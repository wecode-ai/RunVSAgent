import { useState } from "react"
import { vscode } from "@/utils/vscode"
import { ExternalModelWarningDialog } from "@/wecoder/components/chat/ExternalModelWarningDialog"

/**
 * 处理外部模型风险提示弹窗的自定义Hook
 * @param externalRiskNoticeConfirmed 是否已确认外部模型风险提示
 * @param listApiConfigMeta API配置元数据列表
 * @returns 处理API配置选择和渲染风险提示弹窗的方法
 */
export const useGptWarningDialog = (
	externalRiskNoticeConfirmed: boolean | undefined,
	listApiConfigMeta: any[] | undefined,
) => {
	const [showGptWarning, setShowGptWarning] = useState(false)
	const [selectedApiConfigId, setSelectedApiConfigId] = useState<string | null>(null)

	// 切换外部模型时显示风险提示弹窗
	const handleApiConfigSelection = (value: string) => {
		const selectedConfig = listApiConfigMeta?.find((c) => c.id === value)
		if (!externalRiskNoticeConfirmed && selectedConfig?.isExternalModel) {
			setShowGptWarning(true)
			setSelectedApiConfigId(value)
		} else {
			vscode.postMessage({ type: "loadApiConfigurationById", text: value })
		}
	}

	// 渲染外部模型风险提示弹窗
	const renderGptWarningDialog = () => (
		<ExternalModelWarningDialog
			open={showGptWarning}
			onOpenChange={setShowGptWarning}
			apiConfigId={selectedApiConfigId}
			onConfirm={() => setSelectedApiConfigId(null)}
			onCancel={() => setSelectedApiConfigId(null)}
		/>
	)

	return {
		handleApiConfigSelection,
		renderGptWarningDialog,
	}
}

import React from "react"
import { ExternalModelWarningDialog, useGptWarningDialog } from "./ExternalModelWarningDialog"

/**
 * 全局GPT模型风险提示弹窗组件
 * 可以放在应用的根组件中，监听来自后端的消息并显示风险提示
 */
export const GlobalGptWarningDialog: React.FC = () => {
	const { isOpen, setIsOpen, pendingApiConfigId, handleConfirm, handleCancel } = useGptWarningDialog()

	return (
		<ExternalModelWarningDialog
			open={isOpen}
			onOpenChange={setIsOpen}
			apiConfigId={pendingApiConfigId}
			onConfirm={handleConfirm}
			onCancel={handleCancel}
		/>
	)
}

export default GlobalGptWarningDialog

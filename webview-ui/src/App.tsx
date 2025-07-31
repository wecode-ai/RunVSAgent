import React, { useCallback, useEffect, useRef, useState, useMemo } from "react"
import { useEvent } from "react-use"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"

import { ExtensionMessage } from "@roo/ExtensionMessage"
import TranslationProvider from "./i18n/TranslationContext"
import { MarketplaceViewStateManager } from "./components/marketplace/MarketplaceViewStateManager"

import { vscode } from "./utils/vscode"
import { telemetryClient } from "./utils/TelemetryClient"
import { TelemetryEventName } from "@roo-code/types"
import { ExtensionStateContextProvider, useExtensionState } from "./context/ExtensionStateContext"
import ChatView, { ChatViewRef } from "./components/chat/ChatView"
import HistoryView from "./components/history/HistoryView"
import SettingsView, { SettingsViewRef } from "./components/settings/SettingsView"
import WelcomeView from "./components/welcome/WelcomeView"
import McpView from "./components/mcp/McpView"
import { MarketplaceView } from "./components/marketplace/MarketplaceView"
import ModesView from "./components/modes/ModesView"
import { HumanRelayDialog } from "./components/human-relay/HumanRelayDialog"
import { DeleteMessageDialog, EditMessageDialog } from "./components/chat/MessageModificationConfirmationDialog"
import { AccountView } from "./components/account/AccountView"
import { useAddNonInteractiveClickListener } from "./components/ui/hooks/useNonInteractiveClick"
import { TooltipProvider } from "./components/ui/tooltip"
import { STANDARD_TOOLTIP_DELAY } from "./components/ui/standard-tooltip"
import LoginPage from "./wecoder/pages/LoginPage"
import GlobalGptWarningDialog from "./wecoder/components/chat/GlobalExternalModelWarningDialog"
import { useWecodeConnectionStatus, WecodeConnectionStatusView } from "./wecoder/hooks/wecodeConnectionStatus"

type Tab = "settings" | "history" | "mcp" | "modes" | "chat" | "marketplace" | "account" | "login"

interface HumanRelayDialogState {
	isOpen: boolean
	requestId: string
	promptText: string
}

interface DeleteMessageDialogState {
	isOpen: boolean
	messageTs: number
}

interface EditMessageDialogState {
	isOpen: boolean
	messageTs: number
	text: string
	images?: string[]
}

// Memoize dialog components to prevent unnecessary re-renders
const MemoizedDeleteMessageDialog = React.memo(DeleteMessageDialog)
const MemoizedEditMessageDialog = React.memo(EditMessageDialog)
const MemoizedHumanRelayDialog = React.memo(HumanRelayDialog)

const tabsByMessageAction: Partial<Record<NonNullable<ExtensionMessage["action"]>, Tab>> = {
	chatButtonClicked: "chat",
	settingsButtonClicked: "settings",
	promptsButtonClicked: "modes",
	mcpButtonClicked: "mcp",
	historyButtonClicked: "history",
	marketplaceButtonClicked: "marketplace",
	accountButtonClicked: "account",
}

const App = () => {
	const {
		didHydrateState,
		shouldShowAnnouncement,
		telemetrySetting,
		telemetryKey,
		machineId,
		cloudUserInfo,
		cloudIsAuthenticated,
		cloudApiUrl,
		renderContext,
		mdmCompliant,
	} = useExtensionState()

	const { connectionStatus, updateConnectionStatus, retryConnection } = useWecodeConnectionStatus("checking")
	// Create a persistent state manager
	const marketplaceStateManager = useMemo(() => new MarketplaceViewStateManager(), [])

	const [showAnnouncement, setShowAnnouncement] = useState(false)
	const [tab, setTab] = useState<Tab>("chat")
	const [username, setUsername] = useState<string | undefined>(undefined)

	const [humanRelayDialogState, setHumanRelayDialogState] = useState<HumanRelayDialogState>({
		isOpen: false,
		requestId: "",
		promptText: "",
	})

	const [deleteMessageDialogState, setDeleteMessageDialogState] = useState<DeleteMessageDialogState>({
		isOpen: false,
		messageTs: 0,
	})

	const [editMessageDialogState, setEditMessageDialogState] = useState<EditMessageDialogState>({
		isOpen: false,
		messageTs: 0,
		text: "",
		images: [],
	})

	const settingsRef = useRef<SettingsViewRef>(null)
	const chatViewRef = useRef<ChatViewRef>(null)

	const switchTab = useCallback(
		(newTab: Tab) => {
			// Check MDM compliance before allowing tab switching
			if (mdmCompliant === false && newTab !== "account") {
				return
			}

			setCurrentSection(undefined)
			setCurrentMarketplaceTab(undefined)

			if (settingsRef.current?.checkUnsaveChanges) {
				settingsRef.current.checkUnsaveChanges(() => setTab(newTab))
			} else {
				setTab(newTab)
			}
		},
		[mdmCompliant],
	)

	const [currentSection, setCurrentSection] = useState<string | undefined>(undefined)
	const [currentMarketplaceTab, setCurrentMarketplaceTab] = useState<string | undefined>(undefined)

	const onMessage = useCallback(
		(e: MessageEvent) => {
			const message: ExtensionMessage = e.data

			if (message.type === "action" && message.action) {
				// Handle switchTab action with tab parameter
				if (message.action === "switchTab" && message.tab) {
					const targetTab = message.tab as Tab
					switchTab(targetTab)
					setCurrentSection(undefined)
					setCurrentMarketplaceTab(undefined)
				} else {
					// Handle other actions using the mapping
					const newTab = tabsByMessageAction[message.action]
					const section = message.values?.section as string | undefined
					const marketplaceTab = message.values?.marketplaceTab as string | undefined

					if (newTab) {
						switchTab(newTab)
						setCurrentSection(section)
						setCurrentMarketplaceTab(marketplaceTab)
					}
				}
			}

			// 处理 Copilot 连接状态消息
			if (message.type === "wecodeConnectionStatus") {
				if (message.payload?.networkStatus === "ok" || message.payload?.networkStatus === "error") {
					console.info(message)
					updateConnectionStatus(message.payload.networkStatus)
				}
			}

			if (message.type === "showHumanRelayDialog" && message.requestId && message.promptText) {
				const { requestId, promptText } = message
				setHumanRelayDialogState({ isOpen: true, requestId, promptText })
			}

			if (message.type === "showDeleteMessageDialog" && message.messageTs) {
				setDeleteMessageDialogState({ isOpen: true, messageTs: message.messageTs })
			}

			if (message.type === "showEditMessageDialog" && message.messageTs && message.text) {
				setEditMessageDialogState({
					isOpen: true,
					messageTs: message.messageTs,
					text: message.text,
					images: message.images || [],
				})
			}

			if (message.type === "acceptInput") {
				chatViewRef.current?.acceptInput()
			}
		},
		[switchTab, updateConnectionStatus],
	)

	useEvent("message", onMessage)

	useEffect(() => {
		if (shouldShowAnnouncement) {
			setShowAnnouncement(true)
			vscode.postMessage({ type: "didShowAnnouncement" })
		}
	}, [shouldShowAnnouncement])

	useEffect(() => {
		if (didHydrateState) {
			telemetryClient.updateTelemetryState(telemetrySetting, telemetryKey, machineId)
		}
	}, [telemetrySetting, telemetryKey, machineId, didHydrateState])

	// Tell the extension that we are ready to receive messages.
	useEffect(() => vscode.postMessage({ type: "webviewDidLaunch" }), [])

	// Focus the WebView when non-interactive content is clicked (only in editor/tab mode)
	useAddNonInteractiveClickListener(
		useCallback(() => {
			// Only send focus request if we're in editor (tab) mode, not sidebar
			if (renderContext === "editor") {
				vscode.postMessage({ type: "focusPanelRequest" })
			}
		}, [renderContext]),
	)
	// Track marketplace tab views
	useEffect(() => {
		if (tab === "marketplace") {
			telemetryClient.capture(TelemetryEventName.MARKETPLACE_TAB_VIEWED)
		}
	}, [tab])

	// 检查登录状态
	useEffect(() => {
		if (didHydrateState) {
			const handleMessage = (event: MessageEvent) => {
				const message = event.data
				if (message.type === "userInfo") {
					const { username } = message
					setUsername(username)
					if (!username && connectionStatus === "ok") {
						switchTab("login")
					}
				}
			}

			window.addEventListener("message", handleMessage)

			// 获取用户信息
			vscode.postMessage({
				type: "getUserInfo",
			})

			return () => {
				window.removeEventListener("message", handleMessage)
			}
		}
	}, [didHydrateState, switchTab, connectionStatus])

	if (!didHydrateState) {
		return null
	}
	if (connectionStatus !== "ok" && !username) {
		return (
			<WecodeConnectionStatusView
				status={connectionStatus}
				onRetry={retryConnection}
				retrying={connectionStatus === "checking"}
			/>
		)
	}

	const f = false
	return f ? (
		<WelcomeView />
	) : (
		<>
			{tab === "modes" && <ModesView onDone={() => switchTab("chat")} />}
			{tab === "mcp" && <McpView onDone={() => switchTab("chat")} />}
			{tab === "history" && <HistoryView onDone={() => switchTab("chat")} />}
			{tab === "settings" && (
				<SettingsView ref={settingsRef} onDone={() => setTab("chat")} targetSection={currentSection} />
			)}
			{tab === "marketplace" && (
				<MarketplaceView
					stateManager={marketplaceStateManager}
					onDone={() => switchTab("chat")}
					targetTab={currentMarketplaceTab as "mcp" | "mode" | undefined}
				/>
			)}
			{tab === "account" && (
				<AccountView
					userInfo={cloudUserInfo}
					isAuthenticated={cloudIsAuthenticated}
					cloudApiUrl={cloudApiUrl}
					onDone={() => switchTab("chat")}
				/>
			)}
			{tab === "login" && <LoginPage onDone={() => switchTab("chat")} />}
			<ChatView
				ref={chatViewRef}
				isHidden={tab !== "chat"}
				showAnnouncement={showAnnouncement}
				hideAnnouncement={() => setShowAnnouncement(false)}
			/>
			<MemoizedHumanRelayDialog
				isOpen={humanRelayDialogState.isOpen}
				requestId={humanRelayDialogState.requestId}
				promptText={humanRelayDialogState.promptText}
				onClose={() => setHumanRelayDialogState((prev) => ({ ...prev, isOpen: false }))}
				onSubmit={(requestId, text) => vscode.postMessage({ type: "humanRelayResponse", requestId, text })}
				onCancel={(requestId) => vscode.postMessage({ type: "humanRelayCancel", requestId })}
			/>
			<MemoizedDeleteMessageDialog
				open={deleteMessageDialogState.isOpen}
				onOpenChange={(open) => setDeleteMessageDialogState((prev) => ({ ...prev, isOpen: open }))}
				onConfirm={() => {
					vscode.postMessage({
						type: "deleteMessageConfirm",
						messageTs: deleteMessageDialogState.messageTs,
					})
					setDeleteMessageDialogState((prev) => ({ ...prev, isOpen: false }))
				}}
			/>
			<MemoizedEditMessageDialog
				open={editMessageDialogState.isOpen}
				onOpenChange={(open) => setEditMessageDialogState((prev) => ({ ...prev, isOpen: open }))}
				onConfirm={() => {
					vscode.postMessage({
						type: "editMessageConfirm",
						messageTs: editMessageDialogState.messageTs,
						text: editMessageDialogState.text,
						images: editMessageDialogState.images,
					})
					setEditMessageDialogState((prev) => ({ ...prev, isOpen: false }))
				}}
			/>
			{/* 全局GPT模型风险提示弹窗 */}
			<GlobalGptWarningDialog />
		</>
	)
}

const queryClient = new QueryClient()

const AppWithProviders = () => (
	<ExtensionStateContextProvider>
		<TranslationProvider>
			<QueryClientProvider client={queryClient}>
				<TooltipProvider delayDuration={STANDARD_TOOLTIP_DELAY}>
					<App />
				</TooltipProvider>
			</QueryClientProvider>
		</TranslationProvider>
	</ExtensionStateContextProvider>
)

export default AppWithProviders

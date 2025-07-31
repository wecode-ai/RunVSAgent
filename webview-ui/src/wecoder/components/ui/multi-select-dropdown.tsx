import * as React from "react"
import { CaretUpIcon } from "@radix-ui/react-icons"
import { Check, X } from "lucide-react"
import { Fzf } from "fzf"
import { useTranslation } from "react-i18next"

import { cn } from "@/lib/utils"
import { useRooPortal } from "../../../components/ui/hooks/useRooPortal"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui"

export enum MultiDropdownOptionType {
	ITEM = "item",
	SEPARATOR = "separator",
	SHORTCUT = "shortcut",
	ACTION = "action",
}

export interface MultiDropdownOption {
	value: string
	label: string
	disabled?: boolean
	type?: MultiDropdownOptionType
	pinned?: boolean
}

export interface MultiSelectDropdownProps {
	values: string[]
	options: MultiDropdownOption[]
	onChange: (values: string[]) => void
	disabled?: boolean
	title?: string
	triggerClassName?: string
	contentClassName?: string
	itemClassName?: string
	sideOffset?: number
	align?: "start" | "center" | "end"
	placeholder?: string
	shortcutText?: string
	renderItem?: (option: MultiDropdownOption, selected: boolean) => React.ReactNode
}

export const MultiSelectDropdown = React.forwardRef<React.ElementRef<typeof PopoverTrigger>, MultiSelectDropdownProps>(
	(
		{
			values,
			options,
			onChange,
			disabled = false,
			title = "",
			triggerClassName = "",
			contentClassName = "",
			itemClassName = "",
			sideOffset = 4,
			align = "start",
			placeholder = "",
			shortcutText = "",
			renderItem,
		},
		ref,
	) => {
		const { t } = useTranslation()
		const [open, setOpen] = React.useState(false)
		const [searchValue, setSearchValue] = React.useState("")
		// 添加内部选择状态，用于控制UI更新
		const [internalValues, setInternalValues] = React.useState<string[]>(values)
		const searchInputRef = React.useRef<HTMLInputElement>(null)
		const portalContainer = useRooPortal("roo-portal")

		// 当外部values变化时，同步到内部状态
		React.useEffect(() => {
			setInternalValues(values)
		}, [values])

		// 获取选中项的标签
		const selectedLabels = React.useMemo(() => {
			return options.filter((option) => internalValues.includes(option.value)).map((option) => option.label)
		}, [options, internalValues])

		// 显示文本
		const displayText = React.useMemo(() => {
			if (internalValues.length === 0) {
				return placeholder || "知识库：无"
			} else if (internalValues.length === 1) {
				return `知识库：${selectedLabels[0] || ""}`
			} else {
				return `知识库：已选择 ${internalValues.length} 项`
			}
		}, [internalValues, selectedLabels, placeholder])

		// Reset search value when dropdown closes
		const onOpenChange = React.useCallback((open: boolean) => {
			setOpen(open)
			// Clear search when closing
			if (!open) {
				requestAnimationFrame(() => setSearchValue(""))
			}
		}, [])

		// Clear search and focus input
		const onClearSearch = React.useCallback(() => {
			setSearchValue("")
			searchInputRef.current?.focus()
		}, [])

		// Filter options based on search value using Fzf for fuzzy search
		const searchableItems = React.useMemo(() => {
			return options
				.filter(
					(option) =>
						option.type !== MultiDropdownOptionType.SEPARATOR &&
						option.type !== MultiDropdownOptionType.SHORTCUT,
				)
				.map((option) => ({
					original: option,
					searchStr: [option.label, option.value].filter(Boolean).join(" "),
				}))
		}, [options])

		// Create a memoized Fzf instance
		const fzfInstance = React.useMemo(() => {
			return new Fzf(searchableItems, {
				selector: (item) => item.searchStr,
			})
		}, [searchableItems])

		// Filter options based on search value
		const filteredOptions = React.useMemo(() => {
			// If no search value, return all options without filtering
			if (!searchValue) return options

			// Get fuzzy matching items
			const matchingItems = fzfInstance.find(searchValue).map((result) => result.item.original)

			// Always include separators and shortcuts
			return options.filter((option) => {
				if (
					option.type === MultiDropdownOptionType.SEPARATOR ||
					option.type === MultiDropdownOptionType.SHORTCUT
				) {
					return true
				}

				// Include if it's in the matching items
				return matchingItems.some((item) => item.value === option.value)
			})
		}, [options, searchValue, fzfInstance])

		// Group options by type and handle separators
		const groupedOptions = React.useMemo(() => {
			const result: MultiDropdownOption[] = []
			let lastWasSeparator = false

			filteredOptions.forEach((option) => {
				if (option.type === MultiDropdownOptionType.SEPARATOR) {
					// Only add separator if we have items before and after it
					if (result.length > 0 && !lastWasSeparator) {
						result.push(option)
						lastWasSeparator = true
					}
				} else {
					result.push(option)
					lastWasSeparator = false
				}
			})

			// Remove trailing separator if present
			if (result.length > 0 && result[result.length - 1].type === MultiDropdownOptionType.SEPARATOR) {
				result.pop()
			}

			return result
		}, [filteredOptions])

		const handleSelect = React.useCallback(
			(optionValue: string) => {
				const option = options.find((opt) => opt.value === optionValue)

				if (!option) return

				if (option.type === MultiDropdownOptionType.ACTION) {
					window.postMessage({ type: "action", action: option.value })
					setSearchValue("")
					// 不关闭下拉框，允许继续选择
					return
				}

				if (option.disabled) return

				// 多选逻辑
				const newValues = [...internalValues]
				const index = newValues.indexOf(option.value)

				if (index === -1) {
					// 添加选项
					newValues.push(option.value)
				} else {
					// 移除选项
					newValues.splice(index, 1)
				}

				console.log("选择变更:", optionValue, newValues)

				// 先更新内部状态，确保UI立即反映变化
				setInternalValues(newValues)

				// 然后通知父组件
				onChange(newValues)

				// 重置搜索值以触发重新过滤（如果需要）
				if (searchValue) {
					const currentSearch = searchValue
					setSearchValue("")
					setTimeout(() => setSearchValue(currentSearch), 0)
				}
			},
			[onChange, options, internalValues, searchValue],
		)

		return (
			<Popover
				open={open}
				onOpenChange={onOpenChange}
				data-testid="multi-dropdown-root"
				modal={false} // 确保点击内容不会自动关闭弹出框
			>
				<PopoverTrigger
					ref={ref}
					disabled={disabled}
					title={title}
					data-testid="multi-dropdown-trigger"
					className={cn(
						"w-full min-w-0 max-w-full inline-flex items-center gap-1.5 relative whitespace-nowrap px-1.5 py-1 text-xs",
						"bg-transparent border border-[rgba(255,255,255,0.08)] rounded-md text-vscode-foreground w-auto",
						"transition-all duration-150 focus:outline-none focus-visible:ring-1 focus-visible:ring-vscode-focusBorder focus-visible:ring-inset",
						disabled
							? "opacity-50 cursor-not-allowed"
							: "opacity-90 hover:opacity-100 hover:bg-[rgba(255,255,255,0.03)] hover:border-[rgba(255,255,255,0.15)] cursor-pointer",
						triggerClassName,
					)}>
					<CaretUpIcon className="pointer-events-none opacity-80 flex-shrink-0 size-3" />
					<span className="truncate">{displayText}</span>
				</PopoverTrigger>
				<PopoverContent
					align={align}
					sideOffset={sideOffset}
					container={portalContainer}
					className={cn("p-0 overflow-hidden", contentClassName)}>
					<div className="flex flex-col w-full">
						{/* Search input */}
						<div className="relative p-2 border-b border-vscode-dropdown-border">
							<input
								aria-label="Search"
								ref={searchInputRef}
								value={searchValue}
								onChange={(e) => setSearchValue(e.target.value)}
								placeholder={t("common:ui.search_placeholder")}
								className="w-full h-8 px-2 py-1 text-xs bg-vscode-input-background text-vscode-input-foreground border border-vscode-input-border rounded focus:outline-0"
							/>
							{searchValue.length > 0 && (
								<div className="absolute right-4 top-0 bottom-0 flex items-center justify-center">
									<X
										className="text-vscode-input-foreground opacity-50 hover:opacity-100 size-4 p-0.5 cursor-pointer"
										onClick={onClearSearch}
									/>
								</div>
							)}
						</div>

						{/* Dropdown items */}
						<div className="max-h-[300px] overflow-y-auto">
							{groupedOptions.length === 0 && searchValue ? (
								<div className="py-2 px-3 text-sm text-vscode-foreground/70">未找到结果</div>
							) : (
								<div className="py-1">
									{groupedOptions.map((option, index) => {
										if (option.type === MultiDropdownOptionType.SEPARATOR) {
											return (
												<div
													key={`sep-${index}`}
													className="mx-1 my-1 h-px bg-vscode-dropdown-foreground/10"
													data-testid="multi-dropdown-separator"
												/>
											)
										}

										if (
											option.type === MultiDropdownOptionType.SHORTCUT ||
											(option.disabled && shortcutText && option.label.includes(shortcutText))
										) {
											return (
												<div key={`label-${index}`} className="px-3 py-1.5 text-sm opacity-50">
													{option.label}
												</div>
											)
										}

										const itemKey = `item-${option.value || option.label || index}`
										const isSelected = internalValues.includes(option.value)

										return (
											<div
												key={itemKey}
												onClick={() => {
													console.log("点击选项:", option.value)
													if (!option.disabled) handleSelect(option.value)
												}}
												className={cn(
													"px-3 py-1.5 text-sm cursor-pointer flex items-center",
													option.disabled
														? "opacity-50 cursor-not-allowed"
														: "hover:bg-vscode-list-hoverBackground",
													isSelected
														? "bg-vscode-list-activeSelectionBackground text-vscode-list-activeSelectionForeground"
														: "",
													itemClassName,
												)}
												data-testid="multi-dropdown-item">
												{renderItem ? (
													renderItem(option, isSelected)
												) : (
													<>
														<div
															className={cn(
																"mr-2 flex h-4 w-4 items-center justify-center rounded-sm border border-vscode-checkbox-border",
																isSelected
																	? "bg-vscode-checkbox-background"
																	: "bg-transparent",
															)}>
															{isSelected && (
																<Check className="h-3 w-3 text-vscode-checkbox-foreground" />
															)}
														</div>
														<span>{option.label}</span>
													</>
												)}
											</div>
										)
									})}
								</div>
							)}
						</div>
					</div>
				</PopoverContent>
			</Popover>
		)
	},
)

MultiSelectDropdown.displayName = "MultiSelectDropdown"

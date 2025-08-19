#!/bin/bash

# JSON Parser Helper Functions for Bash
# This library provides basic JSON parsing capabilities for shell scripts
# Note: This is a simplified parser for basic JSON structures

# Parse JSON file and extract extensions array
# Usage: parse_extensions_config <json_file> <callback_function>
# The callback function will be called for each extension with parameters:
#   callback_function <name> <url> <version> <description>
parse_extensions_config() {
    local json_file="$1"
    local callback_function="$2"
    
    if [[ ! -f "$json_file" ]]; then
        log_error "JSON configuration file not found: $json_file"
        return 1
    fi
    
    if [[ -z "$callback_function" ]]; then
        log_error "Callback function not specified"
        return 1
    fi
    
    # Use jq if available (recommended)
    if command_exists "jq"; then
        parse_with_jq "$json_file" "$callback_function"
        return $?
    fi
    
    # Fallback to basic bash parsing
    parse_with_bash "$json_file" "$callback_function"
    return $?
}

# Parse JSON using jq (if available)
parse_with_jq() {
    local json_file="$1"
    local callback_function="$2"
    
    log_debug "Using jq for JSON parsing"
    
    # Extract extensions array and call callback for each
    jq -r '.extensions[] | [.name, .url, .version, (.description // "")] | @tsv' "$json_file" | \
    while IFS=$'\t' read -r name url version description; do
        # Skip empty lines
        if [[ -n "$name" ]]; then
            # Call the callback function
            "$callback_function" "$name" "$url" "$version" "$description"
        fi
    done
}

# Basic bash-based JSON parsing (fallback)
parse_with_bash() {
    local json_file="$1"
    local callback_function="$2"
    
    log_debug "Using basic bash parsing for JSON"
    
    local in_extensions=false
    local current_extension=""
    local extension_data=()
    
    while IFS= read -r line; do
        # Remove leading/trailing whitespace
        line=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        
        # Skip empty lines and comments
        if [[ -z "$line" ]] || [[ "$line" =~ ^[[:space:]]*// ]]; then
            continue
        fi
        
        # Check if we're entering extensions array
        if [[ "$line" =~ ^\"extensions\"[[:space:]]*: ]]; then
            in_extensions=true
            continue
        fi
        
        # Check if we're leaving extensions array
        if [[ "$in_extensions" == "true" ]] && [[ "$line" =~ ^\][[:space:]]*$ ]]; then
            in_extensions=false
            continue
        fi
        
        # If we're in extensions array, parse extension objects
        if [[ "$in_extensions" == "true" ]]; then
            # Check for start of extension object
            if [[ "$line" =~ ^\{ ]]; then
                current_extension=""
                extension_data=()
                continue
            fi
            
            # Check for end of extension object
            if [[ "$line" =~ ^\}[[:space:]]*$ ]]; then
                if [[ -n "$current_extension" ]]; then
                    # Parse the collected extension data
                    parse_extension_data "$current_extension" "$callback_function"
                fi
                current_extension=""
                continue
            fi
            
            # Collect extension data
            if [[ -n "$current_extension" ]]; then
                current_extension="${current_extension}${line}"
            else
                current_extension="$line"
            fi
        fi
    done < "$json_file"
}

# Parse individual extension data string
parse_extension_data() {
    local extension_data="$1"
    local callback_function="$2"
    
    # Extract name, url, version, and description using regex
    local name=""
    local url=""
    local version=""
    local description=""
    
    # Extract name
    if [[ "$extension_data" =~ \"name\"[[:space:]]*:[[:space:]]*\"([^\"]+)\" ]]; then
        name="${BASH_REMATCH[1]}"
    fi
    
    # Extract url
    if [[ "$extension_data" =~ \"url\"[[:space:]]*:[[:space:]]*\"([^\"]+)\" ]]; then
        url="${BASH_REMATCH[1]}"
    fi
    
    # Extract version
    if [[ "$extension_data" =~ \"version\"[[:space:]]*:[[:space:]]*\"([^\"]+)\" ]]; then
        version="${BASH_REMATCH[1]}"
    fi
    
    # Extract description
    if [[ "$extension_data" =~ \"description\"[[:space:]]*:[[:space:]]*\"([^\"]*)\" ]]; then
        description="${BASH_REMATCH[1]}"
    fi
    
    # Call callback if we have the required fields
    if [[ -n "$name" ]] && [[ -n "$url" ]] && [[ -n "$version" ]]; then
        "$callback_function" "$name" "$url" "$version" "$description"
    else
        log_warn "Incomplete extension data: name='$name', url='$url', version='$version'"
    fi
}

# Get JSON setting value
# Usage: get_json_setting <json_file> <setting_path>
# Example: get_json_setting "extensions.json" "settings.download_retries"
get_json_setting() {
    local json_file="$1"
    local setting_path="$2"
    
    if [[ ! -f "$json_file" ]]; then
        return 1
    fi
    
    # Use jq if available
    if command_exists "jq"; then
        jq -r ".$setting_path" "$json_file" 2>/dev/null
        return $?
    fi
    
    # Fallback to basic parsing
    get_setting_with_bash "$json_file" "$setting_path"
}

# Basic bash-based setting extraction
get_setting_with_bash() {
    local json_file="$1"
    local setting_path="$2"
    
    # This is a simplified implementation
    # For complex nested paths, jq is recommended
    
    # Split the path
    IFS='.' read -ra path_parts <<< "$setting_path"
    
    # Simple case: just one level
    if [[ ${#path_parts[@]} -eq 1 ]]; then
        local setting_name="${path_parts[0]}"
        grep -o "\"$setting_name\"[[:space:]]*:[[:space:]]*[^,}]*" "$json_file" | \
        sed 's/.*:[[:space:]]*//;s/[[:space:]]*$//;s/^"//;s/"$//'
        return 0
    fi
    
    # For nested paths, we'd need more complex parsing
    log_warn "Complex nested JSON paths require jq for proper parsing"
    return 1
}

# Validate JSON file structure
validate_json_config() {
    local json_file="$1"
    
    if [[ ! -f "$json_file" ]]; then
        log_error "JSON file not found: $json_file"
        return 1
    fi
    
    # Use jq for validation if available
    if command_exists "jq"; then
        if jq empty "$json_file" >/dev/null 2>&1; then
            # Check if it has the required structure
            if jq -e '.extensions' "$json_file" >/dev/null 2>&1; then
                log_debug "JSON file is valid and has extensions array"
                return 0
            else
                log_error "JSON file is valid but missing 'extensions' array"
                return 1
            fi
        else
            log_error "Invalid JSON file: $json_file"
            return 1
        fi
    fi
    
    # Basic validation for bash parsing
    log_warn "jq not available, basic validation only"
    if grep -q '"extensions"' "$json_file"; then
        log_debug "JSON file appears to have extensions array"
        return 0
    else
        log_error "JSON file missing 'extensions' array"
        return 1
    fi
}

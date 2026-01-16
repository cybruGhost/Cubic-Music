// AccountInfoFetcher.kt - FINAL WORKING VERSION
package it.fast4x.rimusic.extensions.youtubelogin

import it.fast4x.innertube.Innertube
import it.fast4x.innertube.models.AccountInfo
import timber.log.Timber

class AccountInfoFetcher {
    
    companion object {
        suspend fun fetchAccountInfo(): AccountInfo? {
            return try {
                Timber.d("AccountInfoFetcher: Starting account info fetch")
                
                // Try to get account menu
                val menuResult = try {
                    Innertube.accountMenu()
                } catch (e: Exception) {
                    Timber.e("AccountInfoFetcher: Error calling accountMenu: ${e.message}")
                    null
                }
                
                // Check if menuResult is an AccountMenuResponse
                if (menuResult is it.fast4x.innertube.models.AccountMenuResponse) {
                    Timber.d("AccountInfoFetcher: Got AccountMenuResponse")
                    
                    // Extract account info using the toAccountInfo() method
                    return menuResult.actions?.firstOrNull()?.openPopupAction?.popup
                        ?.multiPageMenuRenderer?.header?.activeAccountHeaderRenderer?.toAccountInfo()
                }
                
                Timber.d("AccountInfoFetcher: Could not extract account info")
                null
            } catch (e: Exception) {
                Timber.e("AccountInfoFetcher: Error fetching account info: ${e.message}")
                null
            }
        }
    }
}
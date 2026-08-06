@Composable
private fun TradePlaceholder() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(Tokens.Space6),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.SwapHoriz,
            contentDescription = null,
            tint = Tokens.Violet,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(Tokens.Space4))
        Text(
            stringResource(R.string.trade_soon_title),
            color = Tokens.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(Tokens.Space2))
        Text(
            stringResource(R.string.trade_soon_body),
            color = Tokens.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
